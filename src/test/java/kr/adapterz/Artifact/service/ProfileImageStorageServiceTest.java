package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static kr.adapterz.Artifact.response.code.ValidationField.PROFILE_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_TOO_LARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 사용자 폴더 대신 JUnit 임시 폴더를 사용해 이미지 검증과 저장을 확인합니다.
 */
class ProfileImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void 이미지가_없으면_파일을_만들지_않고_null을_반환한다() {
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(tempDirectory.toString());

        assertNull(storageService.store(null));
    }

    @Test
    void 정상_PNG를_UUID_파일명으로_저장하고_공개_경로를_반환한다() throws IOException {
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(tempDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
                "profile_image",
                "original-name.png",
                "image/png",
                createPngBytes()
        );

        String publicPath = storageService.store(image);

        assertNotNull(publicPath);
        assertTrue(publicPath.matches("^/uploads/profiles/[0-9a-f-]+\\.png$"));

        String storedFileName = Path.of(publicPath).getFileName().toString();
        assertTrue(Files.exists(
                tempDirectory.resolve("profiles").resolve(storedFileName)
        ));
    }

    @Test
    void UUID가_중복되면_기존_파일을_유지하고_새_UUID로_재시도한다() throws IOException {
        UUID duplicatedUuid = UUID.fromString(
                "00000000-0000-0000-0000-000000000001"
        );
        UUID nextUuid = UUID.fromString(
                "00000000-0000-0000-0000-000000000002"
        );
        var uuids = List.of(duplicatedUuid, nextUuid).iterator();
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(
                        tempDirectory.toString(),
                        uuids::next
                );
        Path profileDirectory = tempDirectory.resolve("profiles");
        Path existingFile = profileDirectory.resolve(duplicatedUuid + ".png");
        byte[] existingContents = "existing-user-image".getBytes(StandardCharsets.UTF_8);

        Files.createDirectories(profileDirectory);
        Files.write(existingFile, existingContents);

        MockMultipartFile newImage = new MockMultipartFile(
                "profile_image",
                "new-profile.png",
                "image/png",
                createPngBytes()
        );

        String publicPath = storageService.store(newImage);

        assertEquals(
                "/uploads/profiles/" + nextUuid + ".png",
                publicPath
        );
        assertArrayEquals(existingContents, Files.readAllBytes(existingFile));
        assertTrue(Files.exists(profileDirectory.resolve(nextUuid + ".png")));
    }

    @Test
    void UUID가_최대_횟수만큼_중복되면_기존_파일을_유지하고_저장을_중단한다()
            throws IOException {
        UUID duplicatedUuid = UUID.fromString(
                "00000000-0000-0000-0000-000000000003"
        );
        AtomicInteger generationCount = new AtomicInteger();
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(
                        tempDirectory.toString(),
                        () -> {
                            generationCount.incrementAndGet();
                            return duplicatedUuid;
                        }
                );
        Path profileDirectory = tempDirectory.resolve("profiles");
        Path existingFile = profileDirectory.resolve(duplicatedUuid + ".png");
        byte[] existingContents = "must-not-be-deleted".getBytes(StandardCharsets.UTF_8);

        Files.createDirectories(profileDirectory);
        Files.write(existingFile, existingContents);

        MockMultipartFile newImage = new MockMultipartFile(
                "profile_image",
                "new-profile.png",
                "image/png",
                createPngBytes()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> storageService.store(newImage)
        );

        assertEquals("프로필 이미지 파일명을 생성하지 못했습니다.", exception.getMessage());
        assertEquals(5, generationCount.get());
        assertArrayEquals(existingContents, Files.readAllBytes(existingFile));
    }

    @Test
    void MIME만_이미지이고_내용이_이미지가_아니면_거부한다() {
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(tempDirectory.toString());
        MockMultipartFile fakeImage = new MockMultipartFile(
                "profile_image",
                "fake.png",
                "image/png",
                "not-an-image".getBytes(StandardCharsets.UTF_8)
        );

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> storageService.store(fakeImage)
        );

        assertValidationError(exception, PROFILE_IMAGE_INVALID);
    }

    @Test
    void 이미지가_5MB를_초과하면_내용_검사_전에_거부한다() {
        ProfileImageStorageService storageService =
                new ProfileImageStorageService(tempDirectory.toString());
        MockMultipartFile oversizedImage = new MockMultipartFile(
                "profile_image",
                "large.jpg",
                "image/jpeg",
                new byte[(5 * 1024 * 1024) + 1]
        );

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> storageService.store(oversizedImage)
        );

        assertValidationError(exception, PROFILE_IMAGE_TOO_LARGE);
    }

    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(
                2,
                2,
                BufferedImage.TYPE_INT_RGB
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void assertValidationError(
            InvalidInputException exception,
            String reason
    ) {
        assertEquals(1, exception.getErrors().size());
        assertEquals(PROFILE_IMAGE, exception.getErrors().get(0).getField());
        assertEquals(reason, exception.getErrors().get(0).getReason());
    }
}
