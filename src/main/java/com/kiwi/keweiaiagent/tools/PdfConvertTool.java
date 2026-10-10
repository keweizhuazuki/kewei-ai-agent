package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.kiwi.keweiaiagent.constant.FileConstant;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Component
public class PdfConvertTool {

    private static final int DEFAULT_DPI = 200;
    private final Path outputDirectory;

    public PdfConvertTool() {
        this(Path.of(FileConstant.File_SAVE_DIR, "pdf-jpg"));
    }

    PdfConvertTool(Path outputDirectory) {
        this.outputDirectory = outputDirectory.toAbsolutePath().normalize();
    }

    @Tool(description = "Batch convert all PDF files in a directory to JPG images",returnDirect = false)
    public String batchConvertPdfToJpg(
            @ToolParam(description = "Directory path containing PDF files") String pdfDirectoryPath,
            @ToolParam(description = "Optional render DPI, default 200") Integer dpi
    ) {
        if (StrUtil.isBlank(pdfDirectoryPath)) {
            return "Error: pdfDirectoryPath is required.";
        }

        File pdfDir = new File(pdfDirectoryPath);
        if (!pdfDir.exists() || !pdfDir.isDirectory()) {
            return "Error: pdfDirectoryPath is not a valid directory: " + pdfDirectoryPath;
        }

        int finalDpi = (dpi == null || dpi <= 0) ? DEFAULT_DPI : dpi;
        FileUtil.mkdir(outputDirectory.toFile());

        List<File> pdfFiles = FileUtil.loopFiles(pdfDir, pathname -> pathname.isFile()
                && "pdf".equalsIgnoreCase(FileUtil.extName(pathname)));
        if (pdfFiles.isEmpty()) {
            return "No PDF files found in directory: " + pdfDirectoryPath;
        }

        int convertedPdfCount = 0;
        int convertedImageCount = 0;
        StringBuilder failed = new StringBuilder();
        Set<Path> generatedPaths = new HashSet<>();

        for (File pdfFile : pdfFiles) {
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                String baseName = FileUtil.mainName(pdfFile);
                Path relativePath = pdfDir.toPath().toAbsolutePath().normalize()
                        .relativize(pdfFile.toPath().toAbsolutePath().normalize());
                Path targetDirectory = relativePath.getParent() == null ? outputDirectory
                        : outputDirectory.resolve(relativePath.getParent());
                FileUtil.mkdir(targetDirectory.toFile());
                int pageCount = document.getNumberOfPages();
                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                    Path outputPath = targetDirectory.resolve(baseName + "_" + (pageIndex + 1) + ".jpg");
                    if (!generatedPaths.add(outputPath)) {
                        throw new IOException("Conflicting output path: " + outputDirectory.relativize(outputPath));
                    }
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, finalDpi, ImageType.RGB);
                    if (!ImageIO.write(image, "jpg", outputPath.toFile())) {
                        throw new IOException("No JPG writer available");
                    }
                    convertedImageCount++;
                }
                convertedPdfCount++;
            } catch (Exception e) {
                failed.append(pdfFile.getName()).append(": ").append(e.getMessage()).append("; ");
            }
        }

        if (!failed.isEmpty()) {
            return "Batch conversion partially completed. convertedPdf=" + convertedPdfCount
                    + ", convertedImages=" + convertedImageCount
                    + ", outputDir=" + outputDirectory
                    + ", failed=" + failed;
        }

        return "Batch conversion completed. convertedPdf=" + convertedPdfCount
                + ", convertedImages=" + convertedImageCount
                + ", outputDir=" + outputDirectory;
    }
}
