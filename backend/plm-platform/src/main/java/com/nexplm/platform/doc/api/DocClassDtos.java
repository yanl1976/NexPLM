package com.nexplm.platform.doc.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 文档分类 DTO。
 */
public final class DocClassDtos {

    private DocClassDtos() {
    }

    /**
     * 分类树节点；docCount 为该分类（不含子分类）下的文档数。
     * <p>{@code codePath} 是蓝本「关联编码」的等价物（`EXP-20261108-44`）：
     * {@code 种类规则/码值1/码值2/*}，为空表示该分类不自动取号。</p>
     */
    public record ClassNode(Long id, Long parentId, String code, String name, String path,
                            Integer sort, String status, String remark, String codeRuleCode,
                            String codePath, Long docCount, List<ClassNode> children) {
    }

    public record ClassSaveReq(
            @Size(max = 64, message = "分类编码不能超过 64 个字符") String code,
            @NotBlank(message = "分类名称不能为空") @Size(max = 64) String name,
            Long parentId,
            Integer sort,
            String status,
            @Size(max = 500) String remark,
            /** 关联编码规则 code；传空串表示解除关联。 */
            @Size(max = 64, message = "关联编码不能超过 64 个字符") String codeRuleCode,
            /**
             * 编码路径 {@code 种类/码值1/码值2/*}（蓝本 CLASSCODEPATH）；
             * {@code null}=不修改，空串=解除绑定。只有零部件分类支持。
             */
            @Size(max = 500, message = "编码路径不能超过 500 个字符") String codePath) {
    }

    /** 生效属性：自身属性 + 祖先可继承属性；inherited=true 表示来自祖先分类。 */
    public record AttrDef(Long id, String code, String name, String valueType, String enumCode,
                          Boolean required, Boolean inheritable, String defaultValue, Integer sort,
                          Long ownerClassId, String ownerClassName, Boolean inherited) {
    }

    public record AttrItem(
            @NotBlank(message = "属性编码不能为空")
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,63}$", message = "属性编码需以字母开头，仅含字母数字下划线")
            String code,
            @NotBlank(message = "属性名称不能为空") @Size(max = 64) String name,
            @NotBlank(message = "值类型不能为空") String valueType,
            String enumCode,
            Boolean required,
            Boolean inheritable,
            @Size(max = 500) String defaultValue,
            Integer sort) {
    }

    /**
     * 全量替换分类自身属性：空数组表示"清空该分类的全部属性"（语义上合法，故不禁止空列表）。
     */
    public record AttrSaveReq(@NotNull(message = "属性列表不能为空") List<AttrItem> items) {
    }
}
