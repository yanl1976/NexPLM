package com.nexplm.platform.analysis.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * P10-1 统计看板：六域指标卡 + 月度趋势 + 下钻明细。
 *
 * <p>六域（文档/零部件/变更/工艺/任务/质量）的业务语义差别很大，"进行中"的含义各不相同，
 * 因此卡片只保留三段可对齐的口径：<b>总量</b>、<b>未完成</b>、<b>已完成</b>，
 * 另加各域自身有意义的<b>超期数</b>；每域的真实状态分布放在 {@code byStatus} 里如实给出，
 * 不做跨域归一化的假统一（各域的 status 取值见对应迁移脚本的注释）。</p>
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** 单个域的指标卡。 */
    public record DomainCard(String domain,
                             String label,
                             long total,
                             long active,
                             long done,
                             long overdue,
                             int doneRate,
                             long addedInPeriod,
                             List<CountRow> byStatus) {
    }

    /** 分组计数（状态分布）。 */
    public record CountRow(String key, int count) {
    }

    /** 月度趋势：一行一个月，按域给出期间内新增数。 */
    public record TrendRow(String month, Map<String, Long> domains, long total) {
    }

    /** 看板总览。 */
    public record Overview(String from,
                           String to,
                           List<String> domains,
                           List<DomainCard> cards,
                           List<TrendRow> trend) {
    }

    /** 下钻明细行：六域统一成「编码 / 名称 / 状态 / 责任人 / 时间」五列。 */
    public record DrillRow(Long id,
                           String code,
                           String name,
                           String status,
                           String owner,
                           OffsetDateTime time) {
    }
}
