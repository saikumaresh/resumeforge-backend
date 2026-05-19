package com.resumeforge.worker.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Component
public class ResumePDFGenerator {

    private static final Logger log = LoggerFactory.getLogger(ResumePDFGenerator.class);
    private static final String PDF_OUTPUT_DIR = System.getProperty("user.home") + "/resumeforge-pdfs/";

    public String generate(UUID tailoredResumeId, Map<String, String> sections) {
        try {
            Files.createDirectories(Paths.get(PDF_OUTPUT_DIR));
            String filePath = PDF_OUTPUT_DIR + tailoredResumeId + ".pdf";

            Document document = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font headingFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph name = new Paragraph("AI-Tailored Resume", titleFont);
            name.setAlignment(Element.ALIGN_CENTER);
            document.add(name);
            document.add(new Paragraph(" "));

            if (sections.containsKey("summary")) addSection(document, "PROFESSIONAL SUMMARY", sections.get("summary"), headingFont, bodyFont);
            if (sections.containsKey("skills")) addSection(document, "SKILLS", sections.get("skills"), headingFont, bodyFont);
            if (sections.containsKey("experience")) addSection(document, "EXPERIENCE", sections.get("experience"), headingFont, bodyFont);
            if (sections.containsKey("projects")) addSection(document, "PROJECTS", sections.get("projects"), headingFont, bodyFont);
            if (sections.containsKey("education")) addSection(document, "EDUCATION", sections.get("education"), headingFont, bodyFont);

            document.close();
            log.info("PDF generated at {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage());
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addSection(Document doc, String title, String content,
                             Font headingFont, Font bodyFont) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, headingFont);
        doc.add(sectionTitle);
        Chunk line = new Chunk(new com.lowagie.text.pdf.draw.LineSeparator());
        doc.add(new Paragraph(line));
        Paragraph body = new Paragraph(content, bodyFont);
        body.setSpacingBefore(5);
        body.setSpacingAfter(15);
        doc.add(body);
    }
}
