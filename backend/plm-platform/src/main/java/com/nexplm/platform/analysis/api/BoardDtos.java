package com.nexplm.platform.analysis.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * P10-2 自定义看板：看板定义（布局）+ 数据集字典 + 预览数据。
 *
 * <p>看板的组件只声明"用哪个数据集、怎么展示"，不含任何 SQL——数据集是服务端注册的
 * 白名单（{@code BoardService.DATASETS}），这样"能改看板的人"不会顺带获得任意查询能力。</p>
 */
public final class BoardDtos {

    private BoardDtos() {
    }

    /** 一个看板组件。{@code type}：metric（合计卡）/ bar（横向条）/ table（明细表）。 */
    public record Component(String id,
                            String type,
                            String title,
                            String dataset,
                            Integer top,
                            Integer span,
                            Integer order) {
    }

    public record BoardSaveReq(@NotBlank @Size(max = 64) String code,
                               @NotBlank @Size(max = 200) String name,
                               @Size(max = 1000) String description,
                               Boolean shared,
                               List<Component> components) {
    }

    public record BoardRow(Long id,
                           String code,
                           String name,
                           String description,
                           boolean shared,
                           int componentCount,
                           OffsetDateTime updatedAt) {
    }

    public record BoardDetail(Long id,
                              String code,
                              String name,
                              String description,
                              boolean shared,
                              List<Component> components,
                              OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
    }

    /** 数据集字典（前端下拉用）。 */
    public record DatasetMeta(String code, String label, String domain) {
    }

    public record CountRow(String key, int count) {
    }

    /** 单个组件的预览数据；{@code total} 是分组计数之和（metric 组件用它当大数字）。 */
    public record ComponentData(String componentId,
                                String type,
                                String title,
                                String dataset,
                                String domain,
                                long total,
                                List<CountRow> rows) {
    }

    public record BoardData(Long boardId, String boardName, List<ComponentData> components) {
    }
}
