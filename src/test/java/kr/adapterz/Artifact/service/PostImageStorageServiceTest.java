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
import java.util.UUID;

import static kr.adapterz.Artifact.response.code.ValidationField.POST_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_TOO_LARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostImageStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void 정상_PNG를_posts_폴더에_UUID_파일명으로_저장한다() throws IOException {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000010");
        PostImageStorageService storageService =
                new PostImageStorageService(tempDirectory.toString(), () -> uuid);
        MockMultipartFile image = new MockMultipartFile(
                "post_image",
                "post.png",
                "image/png",
                createPngBytes()
        );

        String publicPath = storageService.store(image);

        assertEquals("/uploads/posts/" + uuid + ".png", publicPath);
        assertTrue(Files.exists(
                tempDirectory.resolve("posts").resolve(uuid + ".png")
        ));
    }

    @Test
    void MIME만_이미지이고_실제_내용이_아니면_거부한다() {
        PostImageStorageService storageService =
                new PostImageStorageService(tempDirectory.toString(), UUID::randomUUID);
        MockMultipartFile image = new MockMultipartFile(
                "post_image",
                "fake.png",
                "image/png",
                "not-image".getBytes(StandardCharsets.UTF_8)
        );

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> storageService.store(image)
        );

        assertValidationError(exception, POST_IMAGE_INVALID);
    }

    @Test
    void 이미지가_5MB를_초과하면_거부한다() {
        PostImageStorageService storageService =
                new PostImageStorageService(tempDirectory.toString(), UUID::randomUUID);
        MockMultipartFile image = new MockMultipartFile(
                "post_image",
                "large.jpg",
                "image/jpeg",
                new byte[(5 * 1024 * 1024) + 1]
        );

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> storageService.store(image)
        );

        assertValidationError(exception, POST_IMAGE_TOO_LARGE);
    }

    @Test
    void 공개_경로로_저장된_게시글_이미지를_삭제한다() throws IOException {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000011");
        PostImageStorageService storageService =
                new PostImageStorageService(tempDirectory.toString(), () -> uuid);
        MockMultipartFile image = new MockMultipartFile(
                "post_image",
                "post.png",
                "image/png",
                createPngBytes()
        );
        String publicPath = storageService.store(image);
        Path storedFile = tempDirectory.resolve("posts").resolve(uuid + ".png");

        storageService.delete(publicPath);

        assertFalse(Files.exists(storedFile));
    }

    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void assertValidationError(
            InvalidInputException exception,
            String reason
    ) {
        assertEquals(1, exception.getErrors().size());
        assertEquals(POST_IMAGE, exception.getErrors().getFirst().getField());
        assertEquals(reason, exception.getErrors().getFirst().getReason());
    }
}
