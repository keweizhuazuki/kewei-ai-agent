package com.kiwi.keweiaiagent.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 长期记忆工具的共享文件操作与沙箱校验能力。
 */
final class MemoryToolSupport {

    private final Path memoriesRootPath;

    MemoryToolSupport(Path memoriesRootPath) {
        try {
            Files.createDirectories(memoriesRootPath);
            this.memoriesRootPath = memoriesRootPath.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialize memories root directory", e);
        }
    }

    Path resolve(String relativePath) {
        String path = relativePath == null || relativePath.isBlank() ? "." : relativePath.trim();
        Path resolved = memoriesRootPath.resolve(path).normalize();
        if (!resolved.startsWith(memoriesRootPath)) {
            throw new IllegalArgumentException("Path '" + relativePath + "' is outside memory root");
        }
        // 不能只检查规范化后的路径前缀：符号链接可能指向记忆目录之外。
        Path current = memoriesRootPath;
        rejectSymbolicLink(current);
        for (Path component : memoriesRootPath.relativize(resolved)) {
            current = current.resolve(component);
            rejectSymbolicLink(current);
        }
        return resolved;
    }

    private void rejectSymbolicLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Symbolic links are not allowed in memory paths: " + display(path));
        }
    }

    String display(Path path) {
        if (path.equals(memoriesRootPath)) {
            return ".";
        }
        return memoriesRootPath.relativize(path).toString().replace('\\', '/');
    }

    String listDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException("Memory path does not exist: " + display(directory));
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Memory path is not a directory: " + display(directory));
        }
        try (Stream<Path> stream = Files.walk(directory, 2)) {
            List<String> entries = stream
                    .filter(path -> !path.equals(directory))
                    .sorted()
                    .map(path -> display(path) + (Files.isDirectory(path) ? "/" : ""))
                    .toList();
            if (entries.isEmpty()) {
                return "Memory directory is empty: " + display(directory);
            }
            return entries.stream().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    String renderFileWithLineNumbers(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Memory path does not exist: " + display(filePath));
        }
        if (Files.isDirectory(filePath)) {
            throw new IllegalArgumentException("Memory path is a directory: " + display(filePath));
        }
        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            return "1: ";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(i + 1).append(": ").append(lines.get(i));
        }
        return builder.toString();
    }

    void ensureParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    void writeFile(Path filePath, String content) throws IOException {
        ensureParentDirectory(filePath);
        Files.writeString(filePath, content == null ? "" : content);
    }

    void createFile(Path filePath, String content) throws IOException {
        ensureParentDirectory(filePath);
        Files.writeString(filePath, content == null ? "" : content,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    List<String> readLines(Path filePath) throws IOException {
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new IllegalArgumentException("Memory file does not exist: " + display(filePath));
        }
        return Files.readAllLines(filePath);
    }

    void writeLines(Path filePath, List<String> lines) throws IOException {
        ensureParentDirectory(filePath);
        Files.writeString(filePath, String.join(System.lineSeparator(), lines));
    }

    int countOccurrences(String content, String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target text must not be null or empty");
        }
        int count = 0;
        int fromIndex = 0;
        while (true) {
            int found = content.indexOf(target, fromIndex);
            if (found < 0) {
                return count;
            }
            count++;
            fromIndex = found + target.length();
        }
    }

    void deleteRecursively(Path path) throws IOException {
        rejectMemoryRoot(path);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Memory path does not exist: " + display(path));
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path current : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }

    void move(Path source, Path target) throws IOException {
        rejectMemoryRoot(source);
        rejectMemoryRoot(target);
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Memory path does not exist: " + display(source));
        }
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Memory target already exists: " + display(target));
        }
        ensureParentDirectory(target);
        Files.move(source, target);
    }

    private void rejectMemoryRoot(Path path) {
        if (memoriesRootPath.equals(path)) {
            throw new IllegalArgumentException("Cannot delete or rename the memory root");
        }
    }
}
