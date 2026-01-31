package com.epitech.travelorder.audio.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class PcmUtils {

    private static final int BUFFER_SIZE = 4096;

    public static byte[] convertAudioToPcm(byte[] originalData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalData);
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bais)) {

            AudioFormat sourceFormat = sourceStream.getFormat();

            AudioFormat targetFormat = new AudioFormat(
                    PCM_SIGNED,
                    16000,
                    16,
                    1,
                    2,
                    16000,
                    false
            );

            if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                log.warn("Direct conversion not supported from {} to {}, using intermediate PCM format", sourceFormat, targetFormat);

                AudioFormat intermediateFormat = new AudioFormat(
                        PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false);

                try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(intermediateFormat, sourceStream);
                     AudioInputStream finalStream = AudioSystem.getAudioInputStream(targetFormat, pcmStream);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = finalStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    return baos.toByteArray();
                }
            }

            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = convertedStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("Failed to convert audio to PCM format", e);
            throw new IOException("Failed to convert audio to PCM format", e);
        }
    }
}