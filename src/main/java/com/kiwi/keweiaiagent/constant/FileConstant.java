package com.kiwi.keweiaiagent.constant;

/**
 * 文件保存目录常量类
 */
public interface FileConstant {

    /**
     * 文件保存目录，默认在当前用户目录下的 tmp 目录中
     */
    String File_SAVE_DIR = System.getProperty("user.dir") + "/tmp";
}
