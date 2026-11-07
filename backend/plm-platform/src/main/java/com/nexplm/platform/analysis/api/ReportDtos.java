package com.nexplm.platform.analysis.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 复刻批次 D｜报表管理 DTO（模板 + 数据集字典 + 运行结果）。 */
public final class ReportDtos {

    private ReportDtos() {
    }

    /** 数据集输出列（key 对应 SQL 列别名，label 是表头）。 */
    public record ColumnMeta(String key, String label, String type) {
    }

    /** 参数下拉项。 */
    public record ParamOption(String value, String label) {
    }

    /**
     * 数据集参数定义。
     *
     * <p>{@code type} 只用于前端渲染控件：{@code TEXT} 输入框、{@code DATE} 日期、{@code SELECT} 下拉
     * （选项来自 {@code options}）。模板里只保存「用哪些参数」，选项永远以这里为准。</p>
     */
    public record ParamMeta(String key, String label, String type, List<ParamOption> options) {
    }

    /** 数据集白名单条目（前端据此渲染参数表单与结果列）。 */
    public record DatasetMeta(String key, String label, String domain,
                              List<ColumnMeta> columns, List<ParamMeta> params) {
    }

    /** 模板（含解析后的参数定义与数据集名称）。 */
    public record TemplateRow(Long id, String code, String name, String dataset, String datasetLabel,
                              String category, List<ParamMeta> params, String description,
                              boolean builtIn, int sortNo, OffsetDateTime updatedAt) {
    }

    /** 模板保存入参：{@code params} 只取 key/label，类型与选项以数据集字典为准。 */
    public record TemplateSaveReq(@Size(max = 64) String code,
                                  @NotBlank @Size(max = 200) String name,
                                  @NotBlank String dataset,
                                  @Size(max = 32) String category,
                                  List<ParamReq> params,
                                  @Size(max = 500) String description,
                                  Integer sortNo) {
    }

    public record ParamReq(String key, String label) {
    }

    /** 运行入参：参数值统一按字符串传（日期传 {@code yyyy-MM-dd}）。 */
    public record RunReq(Map<String, Object> params, Integer page, Integer size) {
    }

    /** 运行结果：列由数据集定义（顺序稳定），行是原始列名到值的映射。 */
    public record RunResult(String dataset, String datasetLabel, String templateCode, String templateName,
                            List<ColumnMeta> columns, List<Map<String, Object>> rows,
                            long total, int page, int size) {
    }
}
