package com.nexplm.platform.item.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;

import java.util.List;
import java.util.Locale;

/**
 * BOM 视图口径（设计/工艺/制造）的唯一定义处，供 BOM、替代件、基线等子功能复用。
 */
public final class BomViews {

    public static final List<String> TYPES = List.of("DESIGN", "PROCESS", "MANUFACTURE");

    private BomViews() {
    }

    public static String normalize(String viewType) {
        if (viewType == null || viewType.isBlank()) {
            return "DESIGN";
        }
        String normalized = viewType.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "不支持的 BOM 视图：" + viewType + "，可选：" + String.join("/", TYPES));
        }
        return normalized;
    }
}
