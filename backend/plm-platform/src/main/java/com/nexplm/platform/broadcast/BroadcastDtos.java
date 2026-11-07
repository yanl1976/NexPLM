package com.nexplm.platform.broadcast;

import java.time.OffsetDateTime;

/** 批次 F-2 系统广播 DTO。 */
public final class BroadcastDtos {

    private BroadcastDtos() {
    }

    /** 管理列表行：带已读数。 */
    public record BroadcastRow(Long id, String title, String content, String level,
                               String publishState, OffsetDateTime publishAt,
                               OffsetDateTime expireAt, boolean popup,
                               String createdBy, OffsetDateTime createdAt,
                               OffsetDateTime updatedAt, long readCount) {
    }

    /** 登录后弹窗项（只给必要字段，正文由前端渲染）。 */
    public record PendingItem(Long id, String title, String content, String level,
                              OffsetDateTime publishAt) {
    }

    /**
     * 新建/修改请求。
     *
     * <p>时间用字符串而不是 {@code OffsetDateTime}：Element Plus 的日期选择器给的是本地时间
     * （{@code 2026-11-07 09:00:00}），直接映射到 {@code OffsetDateTime} 会因缺时区偏移解析失败；
     * 由服务端按服务器时区解释，语义更接近"管理员在本地时间 9 点发布"。</p>
     */
    public record SaveReq(String title, String content, String level,
                          String publishAt, String expireAt, Boolean popup) {
    }
}
