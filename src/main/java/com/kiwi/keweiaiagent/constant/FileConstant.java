package com.kiwi.keweiaiagent.constant;

/**
 * 文件保存目录常量类
 */
public interface FileConstant {

    /**
     * 文件保存目录，默认在当前用户目录下的 tmp 目录中
     */
    String File_SAVE_DIR = System.getProperty("user.dir") + "/tmp";

    /**
     * 图片上传目录，固定为 tmp/file
     */
    String IMAGE_UPLOAD_DIR = File_SAVE_DIR + "/file";
}
