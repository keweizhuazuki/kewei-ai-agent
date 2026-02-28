package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.kiwi.keweiaiagent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PdfConvertToolTest {

    @Test
    void shouldConvertAllPdfsInDirectoryToJpg() {
        PdfConvertTool tool = new PdfConvertTool();
        String pdfDir = FileConstant.File_SAVE_DIR + "/pdfs";

        String result = tool.batchConvertPdfToJpg(pdfDir, 150);
        Assertions.assertTrue(result.contains("convertedPdf=2"));

        String outputDir = FileConstant.File_SAVE_DIR + "/pdf-jpg";
        Assertions.assertTrue(FileUtil.exist(outputDir + "/26317000000622280814_1.jpg"));
        Assertions.assertTrue(FileUtil.exist(outputDir + "/26317000000622446844_1.jpg"));
    }
}
