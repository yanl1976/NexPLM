#!/usr/bin/env bash
#
# NexPLM 备份脚本（P10-7）：数据库 + 文件仓，带保留策略。
#
# 为什么数据库和文件一起备：文件仓是内容寻址的物理文件，库里存的是 sha256 与归属关系，
# 只备库会得到一堆"指向不存在文件"的记录，只备文件则无法还原归属，两边必须同一时间点。
#
# 用法：
#   ./deploy/backup.sh
#   BACKUP_ROOT=/data/backup RETAIN_DAYS=30 ./deploy/backup.sh
#
# 从环境变量或 ${APP_DIR}/.env 读取：NEXPLM_DB_URL / NEXPLM_DB_USER / NEXPLM_DB_PASSWORD
#   NEXPLM_FILE_STORAGE=local 时备份 NEXPLM_FILE_ROOT 目录；
#   storage=minio 时文件由对象存储侧做站点复制/版本化，本脚本只备份数据库。
set -euo pipefail

APP_DIR=${APP_DIR:-/opt/nexplm}
BACKUP_ROOT=${BACKUP_ROOT:-${APP_DIR}/backup}
RETAIN_DAYS=${RETAIN_DAYS:-14}
ENV_FILE=${ENV_FILE:-${APP_DIR}/.env}

if [ -f "${ENV_FILE}" ]; then
  # shellcheck disable=SC1090
  set -a && . "${ENV_FILE}" && set +a
fi

DB_URL=${NEXPLM_DB_URL:-jdbc:postgresql://localhost:5432/nexplm}
DB_USER=${NEXPLM_DB_USER:-nexplm}
DB_PASSWORD=${NEXPLM_DB_PASSWORD:-}
FILE_STORAGE=${NEXPLM_FILE_STORAGE:-local}
FILE_ROOT=${NEXPLM_FILE_ROOT:-${APP_DIR}/data/files}

# jdbc:postgresql://host:port/db?param → host / port / db
DB_HOSTPORT=${DB_URL#jdbc:postgresql://}
DB_HOSTPORT=${DB_HOSTPORT%%\?*}
DB_NAME=${DB_HOSTPORT#*/}
DB_HOSTPORT=${DB_HOSTPORT%%/*}
DB_HOST=${DB_HOSTPORT%%:*}
DB_PORT=${DB_HOSTPORT#*:}
# 注意：这里必须写成 if，不能写 `[ ... ] && DB_PORT=5432`
# —— 在 set -e 下，条件为假的整条 && 列表返回非 0，会直接终止脚本
if [ "${DB_PORT}" = "${DB_HOST}" ]; then
  DB_PORT=5432
fi

TS=$(date +%Y%m%d-%H%M%S)
mkdir -p "${BACKUP_ROOT}"
log() { printf '[backup] %s\n' "$*"; }

command -v pg_dump >/dev/null 2>&1 || {
  log "未找到 pg_dump，请安装 postgresql-client"
  exit 1
}

log "备份数据库 ${DB_HOST}:${DB_PORT}/${DB_NAME} → ${BACKUP_ROOT}/db-${TS}.dump"
PGPASSWORD="${DB_PASSWORD}" pg_dump \
  --host="${DB_HOST}" --port="${DB_PORT}" --username="${DB_USER}" \
  --format=custom --compress=6 --no-owner --no-privileges \
  --file="${BACKUP_ROOT}/db-${TS}.dump" "${DB_NAME}"

if [ "${FILE_STORAGE}" = "local" ]; then
  if [ -d "${FILE_ROOT}" ]; then
    log "备份文件仓 ${FILE_ROOT} → ${BACKUP_ROOT}/files-${TS}.tar.gz"
    tar -czf "${BACKUP_ROOT}/files-${TS}.tar.gz" -C "${FILE_ROOT}" .
  else
    log "文件仓目录不存在，跳过：${FILE_ROOT}"
  fi
else
  log "文件存储为 ${FILE_STORAGE}，文件由对象存储侧负责（请确认已开启版本化/站点复制）"
fi

# 保留策略：只清自己的产物，避免误删目录里的其它文件
find "${BACKUP_ROOT}" -maxdepth 1 -name 'db-*.dump' -mtime "+${RETAIN_DAYS}" -print -delete 2>/dev/null || true
find "${BACKUP_ROOT}" -maxdepth 1 -name 'files-*.tar.gz' -mtime "+${RETAIN_DAYS}" -print -delete 2>/dev/null || true

log "完成。当前备份："
ls -lh "${BACKUP_ROOT}" | sed -n '1,20p'
log "恢复数据库：pg_restore --clean --if-exists -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} ${BACKUP_ROOT}/db-${TS}.dump"
