package com.kiwi.keweiaiagent.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 文件与下载工具共用的目录边界检查。 */
final class FileToolPathSupport {
    private final Path root;

    FileToolPathSupport(Path root) {
        try {
            Files.createDirectories(root);
            this.root = root.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialize tool directory", e);
        }
    }

    Path resolveFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        Path relative = Path.of(fileName);
        Path resolved = root.resolve(relative).normalize();
        if (relative.isAbsolute() || !resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("File path is outside tool directory");
        }
        Path current = root;
        rejectSymbolicLink(current);
        for (Path component : root.relativize(resolved)) {
            current = current.resolve(component);
            rejectSymbolicLink(current);
        }
        if (Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("File path is a directory");
        }
        return resolved;
    }

    private void rejectSymbolicLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Symbolic links are not allowed in file paths");
        }
    }
}
