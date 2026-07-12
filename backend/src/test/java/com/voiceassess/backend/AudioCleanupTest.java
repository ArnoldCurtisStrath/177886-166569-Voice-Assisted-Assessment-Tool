package com.voiceassess.backend;

import com.voiceassess.backend.service.AudioCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AudioCleanupTest {

    private final Path audioDir = Path.of(
            System.getProperty("java.io.tmpdir"), "voiceassess", "audio");

    @Test
    void testOldFilesGetPurged() throws Exception {
        Files.createDirectories(audioDir);
        var oldFile = audioDir.resolve("test-cleanup-old.webm");
        Files.writeString(oldFile, "dummy audio data");
        Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(48, ChronoUnit.HOURS)));

        assertTrue(Files.exists(oldFile));

        var service = new AudioCleanupService();
        service.purgeOldAudioFiles();

        assertFalse(Files.exists(oldFile));
    }

    @Test
    void testRecentFilesAreNotPurged() throws Exception {
        Files.createDirectories(audioDir);
        var newFile = audioDir.resolve("test-cleanup-new.webm");
        Files.writeString(newFile, "dummy audio data");

        assertTrue(Files.exists(newFile));

        var service = new AudioCleanupService();
        service.purgeOldAudioFiles();

        assertTrue(Files.exists(newFile));

        Files.deleteIfExists(newFile);
    }

    @Test
    void testNonExistentDirectoryDoesNotThrow() {
        // directory might not exist yet — service should handle gracefully
        var service = new AudioCleanupService();
        assertDoesNotThrow(() -> service.purgeOldAudioFiles());
    }
}
