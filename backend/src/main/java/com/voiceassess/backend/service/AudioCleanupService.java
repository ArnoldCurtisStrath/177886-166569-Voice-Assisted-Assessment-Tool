package com.voiceassess.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Cleans up leftover audio files in the temp directory.
 * Runs daily at 3 AM — deletes files older than 24 hours.
 * This catches anything the immediate cleanup (in TeacherController) missed.
 */
@Service
public class AudioCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AudioCleanupService.class);

    private final Path audioDir = Path.of(
            System.getProperty("java.io.tmpdir"), "voiceassess", "audio");

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldAudioFiles() {
        if (!Files.exists(audioDir)) return;

        var cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int deleted = 0;

        try (var files = Files.list(audioDir)) {
            var it = files.iterator();
            while (it.hasNext()) {
                var file = it.next();
                try {
                    var attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        deleted++;
                    }
                } catch (IOException e) {
                    log.warn("Could not process {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Could not scan audio dir: {}", e.getMessage());
        }

        if (deleted > 0) {
            log.info("Purged {} old audio files", deleted);
        }
    }
}
