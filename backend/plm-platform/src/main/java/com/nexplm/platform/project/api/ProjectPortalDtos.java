package com.nexplm.platform.project.api;

import java.time.LocalDate;
import java.util.List;

/** P7-8 项目门户三视图 DTO。 */
public final class ProjectPortalDtos {

    private ProjectPortalDtos() {
    }

    /**
     * 我的项目卡片：{@code myTaskCount}/{@code myOpenTaskCount} 只算"落在我头上"的任务，
     * 门户第一屏要回答的是"我要做什么"，不是"项目有多少任务"。
     */
    public record PortalProjectCard(Long id, String code, String name, String type, String status,
                                    int progress, LocalDate planStart, LocalDate planEnd,
                                    String managerName, int myTaskCount, int myOpenTaskCount,
                                    boolean manager, boolean member) {
    }

    /**
     * 项目经理视图：进度 vs 时间进度。
     *
     * <p>{@code timeProgress} 为 null 表示计划起止没填全，无法判断快慢；
     * {@code deviation = progress - timeProgress}，负数代表落后。</p>
     */
    public record ManagedProjectRow(Long id, String code, String name, String status, int progress,
                                    LocalDate planStart, LocalDate planEnd, Integer timeProgress,
                                    Integer deviation, int taskCount, int doneTaskCount,
                                    int overdueTaskCount, int pendingTimesheetCount, int memberCount,
                                    String managerName) {
    }

    public record PortfolioStatusCount(String status, int count) {
    }

    public record PortfolioRiskRow(Long id, String code, String name, String status, int progress,
                                   Integer timeProgress, Integer deviation, LocalDate planEnd,
                                   int overdueTaskCount) {
    }

    /** 领导视图：组合统计 + 风险清单（按落后程度排序，最多 10 条）。 */
    public record PortfolioOverview(int totalProjects, int activeProjects, int closedProjects,
                                    int overdueProjects, int avgProgress,
                                    List<PortfolioStatusCount> byStatus,
                                    List<PortfolioRiskRow> risks) {
    }
}
