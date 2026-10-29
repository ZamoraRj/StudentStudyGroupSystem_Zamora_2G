package com.student_study_group_finder.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports the Study Group Activity Report to CSV, PDF, or XLSX.
 * Uses only the JDK so no extra dependencies are required.
 */
public final class ReportExporter {

    /** One report section: a title plus metric/value rows. */
    public record Section(String title, List<String[]> rows) {
    }

    private ReportExporter() {
    }

    /** Dispatches to the CSV, PDF, or XLSX writer based on the file extension. */
    public static void export(List<Section> sections, Path target) throws IOException {
        String name = target.getFileName().toString().toLowerCase();
        if (name.endsWith(".csv")) {
            writeCsv(sections, target);
        } else if (name.endsWith(".pdf")) {
            writePdf(sections, target);
        } else if (name.endsWith(".xlsx")) {
            writeXlsx(sections, target);
        } else {
            throw new IOException("Unsupported report format. Use .csv, .pdf, or .xlsx");
        }
    }

    // ==================== CSV ====================

    private static void writeCsv(List<Section> sections, Path target) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("STUDENT STUDY GROUP ACTIVITY REPORT\n");
        sb.append("Generated,").append(csvCell(LocalDateTime.now().toString())).append("\n\n");
        for (Section section : sections) {
            sb.append(csvCell(section.title())).append("\n");
            for (String[] row : section.rows()) {
                sb.append(csvCell(row[0])).append(",").append(csvCell(row[1])).append("\n");
            }
            sb.append("\n");
        }
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== PDF (minimal, dependency-free) ====================

    private static void writePdf(List<Section> sections, Path target) throws IOException {
        List<String> lines = new ArrayList<>();
        List<Boolean> bold = new ArrayList<>();
        List<Integer> size = new ArrayList<>();
        lines.add("STUDENT STUDY GROUP ACTIVITY REPORT"); bold.add(true); size.add(16);
        lines.add("Generated: " + LocalDateTime.now()); bold.add(false); size.add(10);
        lines.add(""); bold.add(false); size.add(10);
        for (Section section : sections) {
            lines.add(section.title()); bold.add(true); size.add(12);
            for (String[] row : section.rows()) {
                lines.add("  " + row[0] + " : " + row[1]); bold.add(false); size.add(10);
            }
            lines.add(""); bold.add(false); size.add(10);
        }

        List<byte[]> pages = new ArrayList<>();
        StringBuilder current = new StringBuilder("BT\n");
        int y = 800;
        int prevY = 0;
        boolean firstOnPage = true;
        for (int i = 0; i < lines.size(); i++) {
            int fontSize = size.get(i);
            int step = fontSize + 4;
            if (y - step < 50) {
                current.append("ET");
                pages.add(current.toString().getBytes(StandardCharsets.US_ASCII));
                current = new StringBuilder("BT\n");
                y = 800;
                prevY = 0;
                firstOnPage = true;
            }
            y -= step;
            current.append("/").append(bold.get(i) ? "F2" : "F1").append(" ")
                    .append(fontSize).append(" Tf ");
            if (firstOnPage) {
                current.append("50 ").append(y).append(" Td ");
                firstOnPage = false;
            } else {
                current.append("0 ").append(y - prevY).append(" Td ");
            }
            prevY = y;
            current.append("(").append(pdfEscape(lines.get(i))).append(") Tj\n");
        }
        current.append("ET");
        pages.add(current.toString().getBytes(StandardCharsets.US_ASCII));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        // 1: catalog, 2: pages, 3: helvetica, 4: helvetica-bold, 5..: page/content pairs
        offsets.add(0L); // placeholder for object numbering (index 0 unused)
        writeObject(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Kids [");
        for (int i = 0; i < pages.size(); i++) {
            kids.append(5 + i * 2).append(" 0 R ");
        }
        kids.append("] /Count ").append(pages.size()).append(" >>");
        writeObject(out, offsets, 2, kids.toString());
        writeObject(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        writeObject(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        for (int i = 0; i < pages.size(); i++) {
            int pageObj = 5 + i * 2;
            int contentObj = pageObj + 1;
            writeObject(out, offsets, pageObj,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + contentObj + " 0 R >>");
            writeStreamObject(out, offsets, contentObj, pages.get(i));
        }
        long xrefPos = out.size();
        int total = 4 + pages.size() * 2;
        StringBuilder xref = new StringBuilder("xref\n0 " + (total + 1) + "\n");
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= total; i++) {
            xref.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        xref.append("trailer\n<< /Size ").append(total + 1).append(" /Root 1 0 R >>\n");
        xref.append("startxref\n").append(xrefPos).append("\n%%EOF");
        out.write(xref.toString().getBytes(StandardCharsets.US_ASCII));

        try (OutputStream file = Files.newOutputStream(target)) {
            out.writeTo(file);
        }
    }

    private static void writeObject(ByteArrayOutputStream out, List<Long> offsets, int id, String body)
            throws IOException {
        offsets.add((long) out.size());
        if (offsets.size() - 1 != id) {
            throw new IOException("PDF object numbering error");
        }
        out.write((id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeStreamObject(ByteArrayOutputStream out, List<Long> offsets, int id, byte[] data)
            throws IOException {
        offsets.add((long) out.size());
        if (offsets.size() - 1 != id) {
            throw new IOException("PDF object numbering error");
        }
        out.write((id + " 0 obj\n<< /Length " + data.length + " >>\nstream\n")
                .getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
    }

    private static String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    // ==================== XLSX (minimal, dependency-free) ====================

    private static void writeXlsx(List<Section> sections, Path target) throws IOException {
        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sheet.append("<cols><col min=\"1\" max=\"1\" width=\"38\"/><col min=\"2\" max=\"2\" width=\"22\"/></cols>");
        sheet.append("<sheetData>");
        int row = 1;
        row = xlsxRow(sheet, row, new String[]{"STUDENT STUDY GROUP ACTIVITY REPORT", ""}, true);
        row = xlsxRow(sheet, row, new String[]{"Generated", LocalDateTime.now().toString()}, false);
        row = xlsxRow(sheet, row, new String[]{"", ""}, false);
        for (Section section : sections) {
            row = xlsxRow(sheet, row, new String[]{section.title(), ""}, true);
            for (String[] data : section.rows()) {
                row = xlsxRow(sheet, row, data, false);
            }
            row = xlsxRow(sheet, row, new String[]{"", ""}, false);
        }
        sheet.append("</sheetData></worksheet>");

        try (OutputStream file = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(file, StandardCharsets.UTF_8)) {
            zipEntry(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" "
                    + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" "
                    + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" "
                    + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                    + "</Types>");
            zipEntry(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" "
                    + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
                    + "Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            zipEntry(zip, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<sheets><sheet name=\"Activity Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "</workbook>");
            zipEntry(zip, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" "
                    + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" "
                    + "Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" "
                    + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" "
                    + "Target=\"styles.xml\"/>"
                    + "</Relationships>");
            zipEntry(zip, "xl/styles.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                    + "<fonts count=\"2\"><font><sz val=\"11\"/></font>"
                    + "<font><b/><sz val=\"11\"/></font></fonts>"
                    + "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>"
                    + "<borders count=\"1\"><border/></borders>"
                    + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    + "<cellXfs count=\"2\">"
                    + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                    + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                    + "</cellXfs></styleSheet>");
            zipEntry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    private static int xlsxRow(StringBuilder sheet, int row, String[] values, boolean bold) {
        sheet.append("<row r=\"").append(row).append("\">");
        char col = 'A';
        for (String value : values) {
            sheet.append("<c r=\"").append(col).append(row).append("\"");
            if (bold) {
                sheet.append(" s=\"1\"");
            }
            sheet.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(xmlEscape(value)).append("</t></is></c>");
            col++;
        }
        sheet.append("</row>");
        return row + 1;
    }

    private static void zipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
