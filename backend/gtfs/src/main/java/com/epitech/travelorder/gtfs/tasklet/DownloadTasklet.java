package com.epitech.travelorder.gtfs.tasklet;

import com.epitech.travelorder.common.util.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class DownloadTasklet implements Tasklet {

    private static final String ZIP_FILENAME = "data.zip";

    private static final String TXT_EXTENSION = ".txt";

    private static final int BUFFER_SIZE = 8192;

    private final LogContext LOG_CTX = LogContext.of(LogContext.GTFS, "DownloadTasklet");

    @Value("${gtfs.source-url}")
    private String gtfsUrl;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Override
    public RepeatStatus execute(@NonNull final StepContribution contribution, @NonNull final ChunkContext chunkContext) throws Exception {
        log.info(LOG_CTX.format("Starting GTFS download and extraction from URL: {}", gtfsUrl));

        Path directory = createDirectoryIfNotExists();
        File zipFile = downloadZipFile(directory);
        extractTextFiles(zipFile, directory);
        cleanupZipFile(zipFile);

        log.info(LOG_CTX.format("GTFS download and extraction completed successfully."));
        return RepeatStatus.FINISHED;
    }

    private Path createDirectoryIfNotExists() throws Exception {
        Path path = Paths.get(localDirectory);
        Files.createDirectories(path);
        return path;
    }

    private File downloadZipFile(Path directory) throws Exception {
        File zipFile = directory.resolve(ZIP_FILENAME).toFile();

        log.debug(LOG_CTX.format("Downloading GTFS zip file from {}", gtfsUrl));

        try (InputStream in = URI.create(gtfsUrl).toURL().openStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {
            StreamUtils.copy(in, out);
        }

        return zipFile;
    }

    private void extractTextFiles(File zipFile, Path directory) throws Exception {
        log.debug(LOG_CTX.format("Extracting text files from zip"));

        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (isTextFile(entry)) {
                    extractEntry(zipInputStream, entry, directory, buffer);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private boolean isTextFile(ZipEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(TXT_EXTENSION);
    }

    private void extractEntry(ZipInputStream zipInputStream, ZipEntry entry, Path directory, byte[] buffer) throws Exception {
        File outputFile = directory.resolve(entry.getName()).toFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            int length;
            while ((length = zipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
        }

        log.debug(LOG_CTX.format("Extracted file: {}", outputFile.getAbsolutePath()));
    }

    private void cleanupZipFile(File zipFile) {
        if (zipFile.delete()) {
            log.debug(LOG_CTX.format("Deleted temporary zip file: {}", zipFile.getAbsolutePath()));
        } else {
            log.warn(LOG_CTX.format("Failed to delete temporary zip file: {}", zipFile.getAbsolutePath()));
        }
    }
}