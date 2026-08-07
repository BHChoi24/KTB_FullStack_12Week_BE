package kr.adapterz.Artifact.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileImageRecoveryStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void 공개파일을_복구폴더에_복사하고_새_UUID로_복구한다() throws IOException {
        Path uploads = tempDirectory.resolve("uploads");
        Path recovery = tempDirectory.resolve("recovery");
        Path publicDirectory = uploads.resolve("profiles");
        Files.createDirectories(publicDirectory);
        byte[] contents = new byte[]{1, 2, 3, 4};
        Files.write(publicDirectory.resolve("original.png"), contents);

        var uuids = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002")
        ).iterator();
        ProfileImageRecoveryStorageService service =
                new ProfileImageRecoveryStorageService(
                        uploads.toString(),
                        recovery.toString(),
                        uuids::next
                );

        String recoveryKey =
                service.quarantine("/uploads/profiles/original.png");
        assertTrue(Files.exists(recovery.resolve("profiles").resolve(recoveryKey)));

        String restoredPath = service.restore(recoveryKey);
        Path restoredFile = publicDirectory.resolve(
                Path.of(restoredPath).getFileName().toString()
        );
        assertArrayEquals(contents, Files.readAllBytes(restoredFile));

        service.deletePublic("/uploads/profiles/original.png");
        service.deleteRecovery(recoveryKey);
        assertFalse(Files.exists(publicDirectory.resolve("original.png")));
        assertFalse(Files.exists(recovery.resolve("profiles").resolve(recoveryKey)));
    }

    @Test
    void 공개파일이_이미_없으면_복구키를_만들지_않는다() {
        ProfileImageRecoveryStorageService service =
                new ProfileImageRecoveryStorageService(
                        tempDirectory.resolve("uploads").toString(),
                        tempDirectory.resolve("recovery").toString(),
                        UUID::randomUUID
                );

        assertNull(service.quarantine("/uploads/profiles/missing.png"));
    }
}
