package com.nexplm.platform.metadata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 元数据建模 DTO。
 */
public final class MetaDtos {

    private MetaDtos() {
    }

    public record FieldDef(
            @NotBlank(message = "字段编码不能为空")
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,63}$", message = "字段编码需以字母开头，仅含字母数字下划线")
            String code,
            @NotBlank(message = "字段名称不能为空") String name,
            String nameEn,
            @NotBlank(message = "值类型不能为空") String valueType,
            String dataType,
            Integer length,
            Boolean required,
            String defaultValue,
            String enumCode,
            String refEntity,
            Boolean searchable,
            Integer sort,
            String uiJson) {
    }

    public record EntitySummary(String code, String name, String nameEn, String parentCode,
                                String status, Integer versionNo, Long fieldCount,
                                OffsetDateTime updatedAt) {
    }

    public record EntityDetail(String code, String name, String nameEn, String parentCode,
                               Boolean abstractFlag, List<String> capabilities, String versionPolicy,
                               String iterationPolicy, String lifecycleCode, Boolean workflowAllowed,
                               String status, Integer versionNo, List<FieldDef> fields) {
    }

    public record EntitySaveReq(
            @NotBlank(message = "对象类型编码不能为空")
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{1,63}$", message = "类型编码需以字母开头，仅含字母数字")
            String code,
            @NotBlank(message = "对象类型名称不能为空") @Size(max = 200) String name,
            String nameEn,
            String parentCode,
            String lifecycleCode,
            String versionPolicy,
            Boolean workflowAllowed,
            List<String> capabilities,
            List<FieldDef> fields) {
    }

    public record EnumItemDef(String code, String name, Integer sort) {
    }

    public record EnumDetail(String code, String name, List<EnumItemDef> items) {
    }

    public record EnumItemsReq(List<EnumItemDef> items) {
    }

    /** 对象查看器页签定义。 */
    public record TabDef(String code, String name, String tabType, String permissionCode,
                         Integer sort, String configJson) {
    }
}
