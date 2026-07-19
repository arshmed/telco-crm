package com.telcocrm.billingservice.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.alignment.HorizontalAlignment;
import com.lowagie.text.alignment.VerticalAlignment;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.telcocrm.billingservice.dto.InvoiceLineResponse;
import com.telcocrm.billingservice.dto.InvoiceResponse;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale TR = new Locale("tr", "TR");

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.NORMAL, java.awt.Color.DARK_GRAY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.GRAY);
    private static final Font FONT_SECTION_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, java.awt.Color.DARK_GRAY);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, java.awt.Color.GRAY);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL, java.awt.Color.DARK_GRAY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.NORMAL, java.awt.Color.WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, java.awt.Color.DARK_GRAY);
    private static final Font FONT_TOTAL_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, java.awt.Color.DARK_GRAY);
    private static final Font FONT_TOTAL_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, new java.awt.Color(0, 102, 153));
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, java.awt.Color.GRAY);

    private static final java.awt.Color COLOR_PRIMARY = new java.awt.Color(0, 102, 153);
    private static final java.awt.Color COLOR_HEADER_BG = new java.awt.Color(0, 102, 153);
    private static final java.awt.Color COLOR_ROW_ALT = new java.awt.Color(245, 248, 250);
    private static final java.awt.Color COLOR_STATUS_PAID = new java.awt.Color(39, 174, 96);
    private static final java.awt.Color COLOR_STATUS_ISSUED = new java.awt.Color(41, 128, 185);
    private static final java.awt.Color COLOR_STATUS_OVERDUE = new java.awt.Color(231, 76, 60);

    public byte[] generate(InvoiceResponse invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, invoice);
            addInvoiceInfo(document, invoice);
            addLineItems(document, invoice);
            addTotals(document, invoice);
            addFooter(document, invoice);

            document.close();
        } catch (DocumentException e) {
            log.error("Failed to generate PDF for invoice {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("PDF generation failed", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidths(new float[]{60, 40});
        headerTable.setWidthPercentage(100);
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Phrase("FATURA", FONT_TITLE));
        leftCell.addElement(new Phrase("Invoice", FONT_SUBTITLE));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph invoiceNo = new Paragraph(invoice.getInvoiceNumber(), FONT_VALUE);
        invoiceNo.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(invoiceNo);

        Paragraph statusPara = new Paragraph();
        statusPara.setAlignment(Element.ALIGN_RIGHT);
        Phrase statusPhrase = new Phrase(getStatusLabel(invoice.getStatus()), getFontForStatus(invoice.getStatus()));
        statusPara.add(statusPhrase);
        rightCell.addElement(statusPhrase);
        rightCell.addElement(new Paragraph(" ", FONT_FOOTER));

        headerTable.addCell(rightCell);
        document.add(headerTable);
        document.add(new Paragraph(" ", FONT_FOOTER));
    }

    private void addInvoiceInfo(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidths(new float[]{50, 50});
        infoTable.setWidthPercentage(100);
        infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell leftInfo = new PdfPCell();
        leftInfo.setBorder(Rectangle.NO_BORDER);
        leftInfo.addElement(new Phrase("Fatura Tarihi", FONT_LABEL));
        leftInfo.addElement(new Phrase(invoice.getIssuedAt() != null
                ? invoice.getIssuedAt().format(DATETIME_FMT) : "-", FONT_VALUE));
        leftInfo.addElement(new Paragraph(" ", FONT_FOOTER));
        leftInfo.addElement(new Phrase("Dönem Başlangıç", FONT_LABEL));
        leftInfo.addElement(new Phrase(invoice.getPeriodStart() != null
                ? invoice.getPeriodStart().format(DATE_FMT) : "-", FONT_VALUE));
        leftInfo.addElement(new Paragraph(" ", FONT_FOOTER));
        leftInfo.addElement(new Phrase("Abone No", FONT_LABEL));
        leftInfo.addElement(new Phrase(invoice.getSubscriptionId() != null
                ? invoice.getSubscriptionId().toString().substring(0, 8) + "..." : "-", FONT_VALUE));
        infoTable.addCell(leftInfo);

        PdfPCell rightInfo = new PdfPCell();
        rightInfo.setBorder(Rectangle.NO_BORDER);
        rightInfo.addElement(new Phrase("Vade Tarihi", FONT_LABEL));
        rightInfo.addElement(new Phrase(invoice.getDueDate() != null
                ? invoice.getDueDate().format(DATE_FMT) : "-", FONT_VALUE));
        rightInfo.addElement(new Paragraph(" ", FONT_FOOTER));
        rightInfo.addElement(new Phrase("Dönem Bitiş", FONT_LABEL));
        rightInfo.addElement(new Phrase(invoice.getPeriodEnd() != null
                ? invoice.getPeriodEnd().format(DATE_FMT) : "-", FONT_VALUE));
        rightInfo.addElement(new Paragraph(" ", FONT_FOOTER));
        rightInfo.addElement(new Phrase("Müşteri No", FONT_LABEL));
        rightInfo.addElement(new Phrase(invoice.getCustomerNo() != null
                ? invoice.getCustomerNo() : invoice.getCustomerId().toString().substring(0, 8) + "...", FONT_VALUE));
        infoTable.addCell(rightInfo);

        document.add(infoTable);
        document.add(new Paragraph(" ", FONT_FOOTER));
    }

    private void addLineItems(Document document, InvoiceResponse invoice) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Fatura Kalemleri", FONT_SECTION_HEADER);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);
        document.add(new Paragraph(" ", FONT_FOOTER));

        PdfPTable table = new PdfPTable(4);
        table.setWidths(new float[]{45, 15, 20, 20});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);

        addTableHeader(table, "Açıklama");
        addTableHeader(table, "Miktar");
        addTableHeader(table, "Birim Fiyat");
        addTableHeader(table, "Tutar");

        java.util.List<InvoiceLineResponse> lines = invoice.getLines();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                InvoiceLineResponse line = lines.get(i);
                PdfPCell descCell = new PdfPCell(new Phrase(line.getDescription(), FONT_TABLE_CELL));
                descCell.setPadding(8);
                descCell.setBackgroundColor(i % 2 == 0 ? java.awt.Color.WHITE : COLOR_ROW_ALT);
                table.addCell(descCell);

                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(line.getQuantity()), FONT_TABLE_CELL));
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setPadding(8);
                qtyCell.setBackgroundColor(i % 2 == 0 ? java.awt.Color.WHITE : COLOR_ROW_ALT);
                table.addCell(qtyCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(formatCurrency(line.getUnitPrice()), FONT_TABLE_CELL));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setPadding(8);
                priceCell.setBackgroundColor(i % 2 == 0 ? java.awt.Color.WHITE : COLOR_ROW_ALT);
                table.addCell(priceCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(formatCurrency(line.getLineTotal()), FONT_TABLE_CELL));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalCell.setPadding(8);
                totalCell.setBackgroundColor(i % 2 == 0 ? java.awt.Color.WHITE : COLOR_ROW_ALT);
                table.addCell(totalCell);
            }
        }

        if (lines == null || lines.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Kalem bulunmuyor", FONT_TABLE_CELL));
            emptyCell.setColspan(4);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setPadding(12);
            table.addCell(emptyCell);
        }

        document.add(table);
        document.add(new Paragraph(" ", FONT_FOOTER));
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addTotals(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidths(new float[]{70, 30});
        totalsTable.setWidthPercentage(100);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(Rectangle.NO_BORDER);
        totalsTable.addCell(emptyCell);

        PdfPCell totalsCell = new PdfPCell();
        totalsCell.setBorder(Rectangle.NO_BORDER);
        totalsCell.setPadding(5);

        addTotalRow(totalsCell, "Ara Toplam:", formatCurrency(invoice.getSubTotal()));
        addTotalRow(totalsCell, "KDV (" + formatRate(invoice.getTaxRate()) + "):", formatCurrency(invoice.getTaxAmount()));

        Paragraph separator = new Paragraph("─────────────────────────", FONT_FOOTER);
        totalsCell.addElement(separator);

        Paragraph grandTotalLine = new Paragraph();
        grandTotalLine.add(new Phrase("TOPLAM: ", FONT_TOTAL_LABEL));
        grandTotalLine.add(new Phrase(formatCurrency(invoice.getGrandTotal()), FONT_TOTAL_VALUE));
        totalsCell.addElement(grandTotalLine);

        totalsTable.addCell(totalsCell);
        document.add(totalsTable);
        document.add(new Paragraph(" ", FONT_FOOTER));
    }

    private void addTotalRow(PdfPCell cell, String label, String value) {
        PdfPTable row = new PdfPTable(2);
        row.setWidths(new float[]{60, 40});
        row.setWidthPercentage(100);
        row.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row.addCell(valueCell);

        cell.addElement(row);
    }

    private void addFooter(Document document, InvoiceResponse invoice) throws DocumentException {
        document.add(new Paragraph(" ", FONT_FOOTER));
        Paragraph footer = new Paragraph("─────────────────────────────────────────────────────────────────", FONT_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph footerText = new Paragraph();
        footerText.setAlignment(Element.ALIGN_CENTER);
        footerText.add(new Phrase("Bu fatura otomatik olarak oluşturulmuştur. Ödeme完成后 lütfen dekontu saklayınız.", FONT_FOOTER));
        document.add(footerText);

        Paragraph generationTime = new Paragraph();
        generationTime.setAlignment(Element.ALIGN_CENTER);
        generationTime.add(new Phrase("Oluşturulma: " + LocalDateTime.now().format(DATETIME_FMT), FONT_FOOTER));
        document.add(generationTime);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "-";
        return String.format(TR, "%,.2f ₺", amount);
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) return "%0";
        return "%" + rate.stripTrailingZeros().toPlainString();
    }

    private String getStatusLabel(InvoiceStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case PAID -> "ÖDENDİ";
            case ISSUED -> "KESİLDİ";
            case OVERDUE -> "VADESİ GEÇTİ";
            case DRAFT -> "TASLAK";
            case CANCELLED -> "İPTAL";
        };
    }

    private Font getFontForStatus(InvoiceStatus status) {
        java.awt.Color color = switch (status) {
            case PAID -> COLOR_STATUS_PAID;
            case ISSUED -> COLOR_STATUS_ISSUED;
            case OVERDUE -> COLOR_STATUS_OVERDUE;
            default -> java.awt.Color.GRAY;
        };
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL, color);
    }
}
