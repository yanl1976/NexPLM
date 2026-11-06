#!/usr/bin/env bash
#
# NexPLM 升级脚本（P10-7）
#
# 设计原则：
#   1. 升级前必须留得下旧版本 —— 旧 jar 先归档，健康检查失败自动换回去；
#   2. 数据库迁移（Flyway）在应用启动时执行，**回滚 jar 不会回滚数据库**，
#      所以升级前必须先备份库（脚本会调用同目录 backup.sh），这条不能省；
#   3. 不改配置、不动 systemd 单元，只负责"换包 + 重启 + 验活 + 失败回滚"。
#
# 用法：
#   ./deploy/upgrade.sh                      # 用默认路径
#   APP_DIR=/opt/nexplm ./deploy/upgrade.sh  # 指定安装目录
#   SKIP_BACKUP=1 ./deploy/upgrade.sh        # 已单独备份时可跳过（不建议）
#
# 约定：新包放在 ${APP_DIR}/nexplm.jar.new，脚本成功后自动 mv 成 nexplm.jar。
set -euo pipefail

APP_DIR=${APP_DIR:-/opt/nexplm}
JAR=${JAR:-${APP_DIR}/nexplm.jar}
NEW_JAR=${NEW_JAR:-${APP_DIR}/nexplm.jar.new}
SERVICE=${SERVICE:-nexplm}
HEALTH_URL=${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-180}
BACKUP_DIR=${BACKUP_DIR:-${APP_DIR}/backup}
SKIP_BACKUP=${SKIP_BACKUP:-0}

TS=$(date +%Y%m%d-%H%M%S)
ROLLBACK_JAR="${BACKUP_DIR}/nexplm-${TS}.jar"

log() { printf '[upgrade] %s\n' "$*"; }
die() { printf '[upgrade][ERROR] %s\n' "$*" >&2; exit 1; }

[ -f "${NEW_JAR}" ] || die "未找到新包：${NEW_JAR}（请先把新 jar 上传为该文件名）"
command -v systemctl >/dev/null 2>&1 || die "当前环境没有 systemctl，请按 docs/08-运维手册.md 手工升级"

log "目标：APP_DIR=${APP_DIR} SERVICE=${SERVICE} JAR=${JAR}"

# ---------- 1. 备份（数据库 + 当前 jar） ----------
mkdir -p "${BACKUP_DIR}"
if [ -f "${JAR}" ]; then
  cp -p "${JAR}" "${ROLLBACK_JAR}"
  log "已归档当前版本：${ROLLBACK_JAR}"
else
  log "未发现既有 jar，按首次部署处理（无回滚包）"
fi

if [ "${SKIP_BACKUP}" != "1" ]; then
  if [ -x "$(dirname "$0")/backup.sh" ] || [ -f "$(dirname "$0")/backup.sh" ]; then
    log "执行数据库与文件仓备份…"
    APP_DIR="${APP_DIR}" BACKUP_ROOT="${BACKUP_DIR}" bash "$(dirname "$0")/backup.sh" \
      || die "备份失败，已中止升级（宁可升不了，也不能没有可恢复点）"
  else
    log "未找到 backup.sh，跳过自动备份；请确认你已有可恢复的数据库备份"
  fi
fi

# ---------- 2. 停服务 ----------
log "停止服务 ${SERVICE}…"
systemctl stop "${SERVICE}"

# ---------- 3. 换包 ----------
install -m 0644 "${NEW_JAR}" "${JAR}"
rm -f "${NEW_JAR}"
log "新包已就位：${JAR}"

# ---------- 4. 启动并等待健康检查 ----------
log "启动服务 ${SERVICE}…"
systemctl start "${SERVICE}"

start_ts=$(date +%s)
deadline=$(( start_ts + HEALTH_TIMEOUT ))
healthy=0
while [ "$(date +%s)" -lt "${deadline}" ]; do
  if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
    healthy=1
    break
  fi
  sleep 3
done

if [ "${healthy}" = "1" ]; then
  log "健康检查通过（${HEALTH_URL}）"
  curl -fsS "${HEALTH_URL}" || true
  printf '\n'
  log "升级完成，用时约 $(( $(date +%s) - start_ts ))s"
  log "回滚包：${ROLLBACK_JAR}"
  log "提醒：Flyway 迁移已在本次启动执行；如需连数据库一起回滚，请用 ${BACKUP_DIR} 下的备份恢复，"
  log "      并确认没有新版本写入的数据，否则会丢数据。"
  exit 0
fi

# ---------- 5. 失败回滚 ----------
printf '[upgrade][ERROR] %ss 内健康检查未通过，开始回滚\n' "${HEALTH_TIMEOUT}" >&2
systemctl stop "${SERVICE}" || true
if [ -f "${ROLLBACK_JAR}" ]; then
  install -m 0644 "${ROLLBACK_JAR}" "${JAR}"
  log "已还原旧版本 jar"
else
  log "没有可回滚的 jar（首次部署），请手工处理"
fi
systemctl start "${SERVICE}" || true

sleep 5
if curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; then
  log "回滚后服务已恢复。"
else
  log "回滚后仍未恢复，请立即查看日志：journalctl -u ${SERVICE} -n 200 --no-pager"
fi
log "注意：数据库可能已被新版本的 Flyway 迁移修改，回滚 jar 不会撤销这些变更 —— 需人工评估后决定是否用备份恢复。"
exit 1
