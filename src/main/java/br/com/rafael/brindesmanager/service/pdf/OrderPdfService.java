package br.com.rafael.brindesmanager.service.pdf;

import br.com.rafael.brindesmanager.entity.CustomerOrder;
import br.com.rafael.brindesmanager.entity.OrderItem;
import br.com.rafael.brindesmanager.service.CustomerOrderService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderPdfService {

    private final CustomerOrderService customerOrderService;

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateOrderPdf(Long orderId) {
        CustomerOrder order = customerOrderService.findActiveOrderByIdAndCurrentUser(orderId);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addHeader(document);
            addOrderInfo(document, order);
            addCustomerInfo(document, order);
            addItemsTable(document, order);
            addTotals(document, order);
            addNotes(document, order);
            addFooter(document);

            document.close();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao gerar PDF do pedido", ex);
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph title = new Paragraph("RAFAEL BRINDES", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Pedido / Orçamento de Brindes", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
    }

    private void addOrderInfo(Document document, CustomerOrder order) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph section = new Paragraph("Dados do Pedido", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addInfoRow(table, "Código", order.getCode());
        addInfoRow(table, "Status", order.getStatus().name());
        addInfoRow(table, "Data", order.getCreatedAt().format(DATE_TIME_FORMATTER));
        addInfoRow(table, "Vendedor", order.getUser().getName());

        document.add(table);
    }

    private void addCustomerInfo(Document document, CustomerOrder order) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph section = new Paragraph("Dados do Cliente", sectionFont);
        section.setSpacingBefore(16);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});

        addInfoRow(table, "Nome", order.getCustomer().getName());
        addInfoRow(table, "Empresa", valueOrDash(order.getCustomer().getCompanyName()));
        addInfoRow(table, "Documento", valueOrDash(order.getCustomer().getDocument()));
        addInfoRow(table, "E-mail", valueOrDash(order.getCustomer().getEmail()));
        addInfoRow(table, "Telefone", valueOrDash(order.getCustomer().getPhone()));

        document.add(table);
    }

    private void addItemsTable(Document document, CustomerOrder order) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        Paragraph section = new Paragraph("Itens do Pedido", sectionFont);
        section.setSpacingBefore(16);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2.5f, 3f, 0.8f, 1.4f, 1.4f});

        addHeaderCell(table, "Ref.");
        addHeaderCell(table, "Produto");
        addHeaderCell(table, "Descrição");
        addHeaderCell(table, "Qtd");
        addHeaderCell(table, "Vl. Unit.");
        addHeaderCell(table, "Total");

        for (OrderItem item : order.getItems()) {
            addBodyCell(table, item.getProduct().getReference());
            addBodyCell(table, item.getProduct().getName());
            addBodyCell(table, valueOrDash(item.getCustomDescription()));
            addBodyCell(table, String.valueOf(item.getQuantity()));
            addBodyCell(table, formatCurrency(item.getUnitPrice()));
            addBodyCell(table, formatCurrency(item.getTotalPrice()));
        }

        document.add(table);
    }

    private void addTotals(Document document, CustomerOrder order) throws DocumentException {
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph total = new Paragraph("Total: " + formatCurrency(order.getTotalAmount()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(16);
        document.add(total);
    }

    private void addNotes(Document document, CustomerOrder order) throws DocumentException {
        if (order.getNotes() == null || order.getNotes().isBlank()) {
            return;
        }

        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph section = new Paragraph("Observações", sectionFont);
        section.setSpacingBefore(18);
        section.setSpacingAfter(8);
        document.add(section);

        Paragraph notes = new Paragraph(order.getNotes(), bodyFont);
        notes.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(notes);
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

        Paragraph footer = new Paragraph(
                "Documento gerado automaticamente pelo Brindes Manager.",
                footerFont
        );
        footer.setSpacingBefore(24);
        footer.setAlignment(Element.ALIGN_CENTER);

        document.add(footer);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(new Color(240, 240, 240));

        PdfPCell valueCell = new PdfPCell(new Phrase(valueOrDash(value), valueFont));
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(220, 220, 220));

        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPCell cell = new PdfPCell(new Phrase(valueOrDash(value), font));
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "-";
        }

        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}