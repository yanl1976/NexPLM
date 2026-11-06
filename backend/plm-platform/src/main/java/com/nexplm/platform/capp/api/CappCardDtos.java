package com.nexplm.platform.capp.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** P6-4 工艺卡片（模板 + 实例）DTO。 */
public final class CappCardDtos {

    private CappCardDtos() {
    }

    /**
     * 明细列定义。
     *
     * <p>{@code field} 走内置变量（见 {@code CappCardService.STEP_FIELDS}），
     * 留空时用 {@code constValue} 输出固定文本（如让某一列恒为「自检」）。</p>
     */
    public record CardColumn(@Size(max = 32) String key,
                             @NotBlank @Size(max = 40) String title,
                             @Size(max = 40) String field,
                             @Size(max = 40) String constValue,
                             Integer width,
                             String align) {
    }

    /** 表头字段（卡片级）：左侧标签 + 右侧取值变量。 */
    public record HeaderField(@NotBlank @Size(max = 40) String label,
                              @Size(max = 40) String field) {
    }

    /** 卡片版式：标题 + 表头字段 + 明细列 + 表尾。 */
    public record CardLayout(@Size(max = 200) String title,
                             List<HeaderField> headerFields,
                             List<CardColumn> columns,
                             @Size(max = 200) String footerText) {
    }

    /** 模板保存入参。 */
    public record TemplateSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 32) String cardType,
                                  @Size(max = 200) String title,
                                  @NotNull @Valid CardLayout layout,
                                  @Size(max = 500) String remark) {
    }

    /** 模板列表行（{@code cardCount} 为引用该模板版本的卡片数）。 */
    public record TemplateRow(Long id, String code, String name, String cardType, int versionNo,
                              String status, String title, String remark, int cardCount,
                              OffsetDateTime releasedAt, OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
    }

    /** 模板详情（含版式）。 */
    public record TemplateDetail(Long id, String code, String name, String cardType, int versionNo,
                                 String status, String title, CardLayout layout, String remark,
                                 int cardCount, OffsetDateTime releasedAt, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt) {
    }

    /** 卡片列表行。 */
    public record CardRow(Long id, String code, String title, Long templateId, String templateCode,
                          int templateVersion, String templateName, Long routingId, String routingCode,
                          Long objectId, String objectCode, String objectName, int rowCount,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 卡片详情（含版式快照与工序行快照）。 */
    public record CardDetail(Long id, String code, String title, Long templateId, String templateCode,
                             int templateVersion, String templateName, CardLayout layout,
                             Long routingId, String routingCode, Long objectId, String objectCode,
                             String objectName, Map<String, String> fieldValues,
                             List<Map<String, String>> rows, String remark,
                             String routeString,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 卡片生成入参：选一个**已发布**模板版本 + 一条工艺路线。 */
    public record CardSaveReq(@NotNull Long templateId,
                              @NotNull Long routingId,
                              Map<String, String> fieldValues,
                              @Size(max = 500) String remark) {
    }
}
