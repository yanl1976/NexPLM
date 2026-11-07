package com.nexplm.platform.iam.api;

import java.time.OffsetDateTime;

/** 批次 F-1 在线用户 DTO（字段对齐旧系统 OnlineUserList 的列，见 docs/06 EXP-20261107-16）。 */
public final class SessionDtos {

    private SessionDtos() {
    }

    public record SessionRow(Long id, Long userId, String username, String displayName,
                             String deviceType, String ip, String ua,
                             OffsetDateTime loginAt, OffsetDateTime lastActiveAt,
                             long onlineSeconds, boolean active) {
    }
}
