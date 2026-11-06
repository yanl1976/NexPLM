package com.nexplm.platform.capp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P6-5 工艺成册与齐套性检查 DTO。 */
public final class CappVolumeDtos {

    private CappVolumeDtos() {
    }

    /** 成册列表行。 */
    public record VolumeRow(Long id, String code, String name, Long objectId, String objectCode,
                            String objectName, String status, int cardCount, String remark,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 册内条目（seqNo 即页序，10/20/30 步进）。 */
    public record VolumeCardRow(int seqNo, Long cardId, String cardCode, String cardTitle,
                                String templateName, int templateVersion, String routingCode,
                                int rowCount) {
    }

    /** 成册详情。 */
    public record VolumeDetail(Long id, String code, String name, Long objectId, String objectCode,
                               String objectName, String status, String remark,
                               List<VolumeCardRow> cards,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 成册保存入参：{@code cardIds} 的顺序即页序。 */
    public record VolumeSaveReq(@NotBlank @Size(max = 200) String name,
                                @NotNull Long objectId,
                                List<Long> cardIds,
                                @Size(max = 500) String remark) {
    }

    /** 齐套检查单项：status = PASS / WARN / FAIL。 */
    public record CheckItem(String key, String label, String status, String detail) {
    }

    /**
     * 齐套性检查结果。
     *
     * <p>{@code ready} 只要求没有 FAIL（有路线、有工序）；工时未填、资源未绑、卡片未生成等
     * 只是 WARN——它们代表「工艺还没写完」，不代表数据错误。</p>
     */
    public record CompletenessResult(Long objectId, String objectCode, String objectName,
                                     int routeCount, int releasedRouteCount,
                                     int stepCount, int substepCount, int cardCount,
                                     int zeroMinuteSteps, int unboundResourceSteps,
                                     List<CheckItem> checks,
                                     int passCount, int warnCount, int failCount, boolean ready) {
    }
}
