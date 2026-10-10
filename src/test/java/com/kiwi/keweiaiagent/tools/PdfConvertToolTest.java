package com.kiwi.keweiaiagent.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfConvertToolTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldConvertRootPdfsWithExistingOutputNames() throws Exception {
        Path input = Files.createDirectory(tempDir.resolve("pdfs"));
        Path output = tempDir.resolve("images");
        createPdf(input.resolve("first.pdf"), 72);
        createPdf(input.resolve("second.pdf"), 144);

        String result = new PdfConvertTool(output).batchConvertPdfToJpg(input.toString(), 72);

        assertTrue(result.contains("convertedPdf=2"), result);
        assertTrue(result.contains("convertedImages=2"), result);
        assertEquals(72, ImageIO.read(output.resolve("first_1.jpg").toFile()).getWidth());
        assertEquals(144, ImageIO.read(output.resolve("second_1.jpg").toFile()).getWidth());
    }

    @Test
    void shouldPreserveSubdirectoriesForPdfsWithTheSameName() throws Exception {
        Path input = Files.createDirectory(tempDir.resolve("pdfs"));
        Path output = tempDir.resolve("images");
        createPdf(input.resolve("first/report.pdf"), 72);
        createPdf(input.resolve("second/report.pdf"), 144);

        String result = new PdfConvertTool(output).batchConvertPdfToJpg(input.toString(), 72);

        assertTrue(result.contains("convertedPdf=2"), result);
        assertEquals(72, ImageIO.read(output.resolve("first/report_1.jpg").toFile()).getWidth());
        assertEquals(144, ImageIO.read(output.resolve("second/report_1.jpg").toFile()).getWidth());
        assertFalse(Files.exists(output.resolve("report_1.jpg")));
    }

    @Test
    void shouldReportNameCollisionsRatherThanOverwriteAnEarlierImage() throws Exception {
        Path input = Files.createDirectory(tempDir.resolve("pdfs"));
        Path output = tempDir.resolve("images");
        createPdf(input.resolve("report.pdf"), 72);
        createPdf(input.resolve("report.PDF"), 144);
        assumeTrue(!Files.isSameFile(input.resolve("report.pdf"), input.resolve("report.PDF")),
                "Requires a case-sensitive filesystem");

        String result = new PdfConvertTool(output).batchConvertPdfToJpg(input.toString(), 72);

        assertTrue(result.contains("Conflicting output path"), result);
        assertTrue(result.contains("convertedPdf=1"), result);
        assertTrue(result.contains("convertedImages=1"), result);
    }

    @Test
    void shouldReportEmptyAndInvalidInputs() throws Exception {
        PdfConvertTool tool = new PdfConvertTool(tempDir.resolve("images"));
        assertTrue(tool.batchConvertPdfToJpg("", null).contains("is required"));
        assertTrue(tool.batchConvertPdfToJpg(tempDir.resolve("missing").toString(), null).contains("not a valid directory"));
        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        assertTrue(tool.batchConvertPdfToJpg(empty.toString(), null).startsWith("No PDF files found"));
    }

    private void createPdf(Path path, float width) throws Exception {
        Files.createDirectories(path.getParent());
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(new PDRectangle(width, 72)));
            document.save(path.toFile());
        }
    }
}
