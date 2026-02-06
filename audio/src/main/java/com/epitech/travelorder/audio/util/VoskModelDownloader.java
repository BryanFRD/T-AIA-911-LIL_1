package com.epitech.travelorder.audio.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public final class VoskModelDownloader {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 300_000;
    private VoskModelDownloader() {
    }

    public static void ensureModelAvailable(String modelPath, String modelUrl) throws IOException {
        Path path = Path.of(modelPath);
        if (isModelPresent(path)) {
            log.info("Vosk model already present at {}", modelPath);
            return;
        }

        log.info("Vosk model not found at {}. Downloading from {}", modelPath, modelUrl);
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        downloadAndExtract(modelUrl, parentDir != null ? parentDir : Path.of("."));
        if (!isModelPresent(path)) {
            throw new IOException("Model download completed but model not found at expected path: " + modelPath);
        }
        log.info("Vosk model successfully downloaded and extracted to {}", modelPath);
    }

    private static boolean isModelPresent(Path modelPath) {
        return Files.isDirectory(modelPath) && Files.exists(modelPath.resolve("conf/model.conf"));
    }

    private static void downloadAndExtract(String modelUrl, Path targetDir) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(modelUrl).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "TravelOrder/1.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download model: HTTP " + responseCode);
        }

        long totalSize = connection.getContentLengthLong();
        log.info("Downloading Vosk model ({} MB)", totalSize > 0 ? totalSize / (1024 * 1024) : "unknown");

        try (InputStream inputStream = connection.getInputStream();
             ZipInputStream zipIn = new ZipInputStream(inputStream)) {
            long bytesRead = 0;
            int lastReportedPercent = -1;
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }
                    long entryBytes = Files.copy(zipIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    bytesRead += entryBytes;

                    if (totalSize > 0) {
                        int percent = (int) (bytesRead * 100 / totalSize);
                        if (percent >= lastReportedPercent + 10) {
                            lastReportedPercent = percent;
                            log.info("Download progress: ~{}%", Math.min(percent, 100));
                        }
                    }
                }
                zipIn.closeEntry();
            }
            log.info("Extraction complete. Total extracted: {} MB", bytesRead / (1024 * 1024));
        } finally {
            connection.disconnect();
        }
    }
}
