package com.nexplm.platform.capp.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappCardDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 工艺卡片 PDF 导出（P6-4b）。
 *
 * <p>版式与 xlsx 导出口径一致：标题 → 表头字段区 → 明细表 → 表尾，数据全部取自卡片的
 * <b>自有快照</b>（`layout_json` / `field_values` / `rows_json`），不回查模板与路线。</p>
 *
 * <p><b>中文字体</b>是这类导出的唯一硬约束：PDF 必须嵌入字体才能跨机器显示中文。
 * 因此：① 优先用配置 {@code nexplm.capp.pdf.font-path}（支持 .ttf / .ttc，ttc 需带索引
 * 形如 {@code simsun.ttc,0}）；② 未配置时按「Windows 宋体/雅黑/黑体 → Linux Noto/文泉驿/AR PL」
 * 顺序探测；③ 全都没有则明确报错（提示配置路径），<b>绝不生成一堆乱码方块</b>。</p>
 */
@Service
public class CappCardPdfService {

    /** 常见中文字体候选（.ttc 需带 ",0" 索引，OpenPDF 才能取到第一个字体）。 */
    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
            "/usr/share/fonts/truetype/arphic/uming.ttc,0"
    };

    private static final Color HEADER_BG = new Color(0xEB, 0xEE, 0xF5);
    private static final Color LABEL_BG = new Color(0xF5, 0xF7, 0xFA);

    private final String configuredFontPath;

    /** 字体加载一次后缓存（createFont 解析 TTC 有开销，且 PDF 导出是低频操作）。 */
    private volatile BaseFont cachedFont;

    public CappCardPdfService(@Value("${nexplm.capp.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    /** 按卡片自带的版式快照渲染单张 PDF。 */
    public byte[] export(CappCardDtos.CardDetail card) {
        BaseFont baseFont = chineseFont();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 26, 26);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            renderCard(document, card, baseFont);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            if (document.isOpen()) {
                document.close();
            }
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "导出 PDF 失败：" + ex.getMessage());
        }
    }

    /**
     * 成册导出：封页（册名/零件/卡片数）+ 目录 + 每张卡各自新起一页。
     *
     * <p>不写页码：页码要在两遍渲染后才能确定，而目录页码对工艺卡的实用性远低于实现成本；
     * 代替方案是目录里保留页序（1./2./3.），与册内 seq_no 一一对应。</p>
     */
    public byte[] exportVolume(String volumeName, String objectLabel, List<CappCardDtos.CardDetail> cards) {
        if (cards == null || cards.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "成册里没有卡片，无法导出");
        }
        BaseFont baseFont = chineseFont();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 26, 26);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(nullToEmpty(volumeName), font(baseFont, 22, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(140f);
            title.setSpacingAfter(18f);
            document.add(title);

            Paragraph object = new Paragraph("零件：" + nullToEmpty(objectLabel),
                    font(baseFont, 13, Font.NORMAL));
            object.setAlignment(Element.ALIGN_CENTER);
            document.add(object);

            Paragraph count = new Paragraph("共 " + cards.size() + " 张工艺卡片",
                    font(baseFont, 12, Font.NORMAL));
            count.setAlignment(Element.ALIGN_CENTER);
            document.add(count);

            document.newPage();
            Paragraph catalogTitle = new Paragraph("目  录", font(baseFont, 16, Font.BOLD));
            catalogTitle.setAlignment(Element.ALIGN_CENTER);
            catalogTitle.setSpacingAfter(12f);
            document.add(catalogTitle);
            int index = 1;
            for (CappCardDtos.CardDetail card : cards) {
                document.add(new Paragraph(index + ".  " + nullToEmpty(card.code()) + "  "
                        + nullToEmpty(card.title()) + "（" + nullToEmpty(card.objectCode()) + "）",
                        font(baseFont, 11, Font.NORMAL)));
                index++;
            }

            for (CappCardDtos.CardDetail card : cards) {
                document.newPage();
                renderCard(document, card, baseFont);
            }
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            if (document.isOpen()) {
                document.close();
            }
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "导出成册 PDF 失败：" + ex.getMessage());
        }
    }

    /** 单张卡片内容（标题 / 表头字段 / 明细 / 表尾），单张导出与成册导出共用。 */
    private void renderCard(Document document, CappCardDtos.CardDetail card, BaseFont baseFont)
            throws Exception {
        CappCardDtos.CardLayout layout = card.layout();
        List<CappCardDtos.CardColumn> columns = layout == null || layout.columns() == null
                ? List.of() : layout.columns();
        if (columns.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "卡片没有明细列，无法导出");
        }
        if (layout.title() != null && !layout.title().isBlank()) {
            Paragraph title = new Paragraph(layout.title(), font(baseFont, 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);
        }
        List<CappCardDtos.HeaderField> headers = layout.headerFields() == null
                ? List.of() : layout.headerFields();
        if (!headers.isEmpty()) {
            document.add(headerTable(headers,
                    card.fieldValues() == null ? Map.of() : card.fieldValues(), baseFont));
        }
        document.add(detailTable(columns, card.rows() == null ? List.of() : card.rows(), baseFont));
        if (layout.footerText() != null && !layout.footerText().isBlank()) {
            Paragraph footer = new Paragraph(layout.footerText(), font(baseFont, 10, Font.NORMAL));
            footer.setSpacingBefore(12f);
            document.add(footer);
        }
    }

    /** 表头字段区：左列标签（灰底）固定窄，右列取值自适应。 */
    private PdfPTable headerTable(List<CappCardDtos.HeaderField> headers, Map<String, String> values,
                                  BaseFont baseFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1f, 3.2f});
        } catch (Exception ignored) {
            // 宽度设置失败不阻断导出，用默认均分
        }
        for (CappCardDtos.HeaderField header : headers) {
            PdfPCell label = textCell(nullToEmpty(header.label()), font(baseFont, 10, Font.BOLD),
                    Element.ALIGN_CENTER, LABEL_BG);
            table.addCell(label);
            String value = header.field() == null ? "" : values.getOrDefault(header.field(), "");
            table.addCell(textCell(value, font(baseFont, 10, Font.NORMAL), Element.ALIGN_LEFT, null));
        }
        if (headers.size() % 2 != 0) {
            table.addCell(textCell("", font(baseFont, 10, Font.NORMAL), Element.ALIGN_LEFT, null));
        }
        table.setSpacingAfter(8f);
        return table;
    }

    /** 明细表：列宽取模板 width（相对比例），数据行按列对齐，合计行加粗。 */
    private PdfPTable detailTable(List<CappCardDtos.CardColumn> columns,
                                  List<Map<String, String>> rows, BaseFont baseFont) {
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        try {
            float[] widths = new float[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                Integer width = columns.get(i).width();
                widths[i] = width == null ? 12f : Math.max(width, 4);
            }
            table.setWidths(widths);
        } catch (Exception ignored) {
            // 同上，宽度异常不影响导出
        }

        Font headFont = font(baseFont, 9, Font.BOLD);
        for (CappCardDtos.CardColumn column : columns) {
            table.addCell(textCell(nullToEmpty(column.title()), headFont, Element.ALIGN_CENTER, HEADER_BG));
        }
        // 工序多的卡片会跨页：表头行每页重复，否则第 2 页起全是无列名的数字
        table.setHeaderRows(1);

        for (Map<String, String> row : rows) {
            boolean summary = "true".equalsIgnoreCase(row.get("__summary__"));
            Font rowFont = font(baseFont, 9, summary ? Font.BOLD : Font.NORMAL);
            for (CappCardDtos.CardColumn column : columns) {
                String value = row.getOrDefault(CappCardService.columnKey(column), "");
                table.addCell(textCell(nullToEmpty(value), rowFont, alignOf(column), null));
            }
        }
        return table;
    }

    private static PdfPCell textCell(String text, Font font, int align, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        return cell;
    }

    private static int alignOf(CappCardDtos.CardColumn column) {
        String align = column.align() == null ? "LEFT" : column.align().toUpperCase();
        return switch (align) {
            case "CENTER" -> Element.ALIGN_CENTER;
            case "RIGHT" -> Element.ALIGN_RIGHT;
            default -> Element.ALIGN_LEFT;
        };
    }

    private static Font font(BaseFont baseFont, float size, int style) {
        return new Font(baseFont, size, style);
    }

    // ------------------------------------------------------------------ 字体

    private BaseFont chineseFont() {
        BaseFont local = cachedFont;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedFont != null) {
                return cachedFont;
            }
            String path = resolveFontPath();
            try {
                cachedFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return cachedFont;
            } catch (Exception ex) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                        "中文字体加载失败：" + path + "（" + ex.getMessage() + "）");
            }
        }
    }

    /** 配置优先，其次按候选列表探测第一个存在的文件。 */
    private String resolveFontPath() {
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            return configuredFontPath.trim();
        }
        for (String candidate : FONT_CANDIDATES) {
            String file = candidate.contains(",") ? candidate.substring(0, candidate.indexOf(',')) : candidate;
            if (new File(file).isFile()) {
                return candidate;
            }
        }
        throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                "未找到可用的中文字体，无法导出 PDF。请配置 nexplm.capp.pdf.font-path"
                        + "（.ttf 或 .ttc，ttc 需带索引如 simsun.ttc,0）");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
