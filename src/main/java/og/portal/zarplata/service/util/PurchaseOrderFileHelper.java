package og.portal.zarplata.service.util;

import og.portal.zarplata.dto.FileNodeDTO;
import og.portal.zarplata.dto.NamedInputStreamDTO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PurchaseOrderFileHelper {

    private static final String TEMP_FILE_PREFIX = "~";
    private static final String BACKSLASH = "\\";
    private static final String FORWARD_SLASH = "/";
    private static final String EXCEL_EXTENSION = ".xlsx";

    public static List<FileNodeDTO> listFiles(String rootPathStr, String relativePath) {
        Path rootPath = Paths.get(rootPathStr);
        Path targetPath = rootPath.resolve(relativePath == null ? "" : relativePath).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Access denied: path is outside of the allowed directory.");
        }

        File directory = targetPath.toFile();
        if (!directory.exists() || !directory.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        WindowsExplorerComparator comparator = new WindowsExplorerComparator();

        return Arrays.stream(files)
                .filter(file -> !file.getName().startsWith(TEMP_FILE_PREFIX))
                .map(file -> new FileNodeDTO(
                        file.getName(),
                        file.isDirectory(),
                        rootPath.relativize(file.toPath()).toString().replace(BACKSLASH, FORWARD_SLASH)
                ))
                .sorted((f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return comparator.compare(f1.getName(), f2.getName());
                })
                .collect(Collectors.toList());
    }

    public static List<File> resolveFiles(String rootPathStr, String currentPath, List<String> fileNames) {
        Path rootPath = Paths.get(rootPathStr);
        Path targetDir = rootPath.resolve(currentPath == null ? "" : currentPath).normalize();

        if (!targetDir.startsWith(rootPath)) {
            throw new IllegalArgumentException("Access denied: path is outside of the allowed directory.");
        }

        List<File> filesToProcess = new ArrayList<>();
        for (String fileName : fileNames) {
            File file = targetDir.resolve(fileName).toFile();
            if (file.exists() && file.isFile() && file.getName().endsWith(EXCEL_EXTENSION)) {
                filesToProcess.add(file);
            }
        }
        return filesToProcess;
    }

    public static List<NamedInputStreamDTO> createInputStreams(List<File> files) throws IOException {
        List<NamedInputStreamDTO> streams = new ArrayList<>();
        for (File file : files) {
            streams.add(new NamedInputStreamDTO(file.getName(), new FileInputStream(file)));
        }
        return streams;
    }
}
