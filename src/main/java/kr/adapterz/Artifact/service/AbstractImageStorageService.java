package kr.adapterz.Artifact.service;

import kr.adapterz.Artifact.exception.InvalidInputException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 프로필과 게시글 이미지가 공통으로 사용하는 로컬 파일 저장 기능입니다.
 *
 * <p>용도별 서비스가 폴더명과 오류 코드를 전달하고, 이 클래스는 파일 검증,
 * UUID 원자적 생성, 저장 및 삭제를 담당합니다.</p>
 */
abstract class AbstractImageStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_UUID_GENERATION_ATTEMPTS = 5;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private final Path imageDirectory;
    private final String publicPath;
    private final String validationField;
    private final String invalidReason;
    private final String tooLargeReason;
    private final String imageLabel;
    private final Supplier<UUID> uuidSupplier;

    protected AbstractImageStorageService(
            String uploadDirectory,
            String subDirectory,
            String publicPath,
            String validationField,
            String invalidReason,
            String tooLargeReason,
            String imageLabel,
            Supplier<UUID> uuidSupplier
    ) {
        this.imageDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve(subDirectory);
        this.publicPath = publicPath;
        this.validationField = validationField;
        this.invalidReason = invalidReason;
        this.tooLargeReason = tooLargeReason;
        this.imageLabel = imageLabel;
        this.uuidSupplier = uuidSupplier;
    }

    /**
     * 선택 이미지를 검증하고 UUID 파일명으로 저장한 다음 공개 상대 경로를 반환합니다.
     * 파일을 선택하지 않은 경우에는 null을 반환합니다.
     */
    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        String extension = validateAndGetExtension(image);
        createImageDirectory();

        // 이미지 이름변환, 저장파일, 중복확인
        for (int attempt = 0; attempt < MAX_UUID_GENERATION_ATTEMPTS; attempt++) {
            // UUID이름으로 변환 + 확장자
            String storedFileName = uuidSupplier.get() + "." + extension;
            // 실제 파일저장 경로
            Path target = imageDirectory.resolve(storedFileName).normalize();

            // UUID를 사용하더라도 저장 폴더 밖으로 벗어나는 경로는 허용하지 않습니다.
            if (!target.startsWith(imageDirectory)) {
                throw new InvalidInputException(validationField, invalidReason);
            }
            // 중복 여부 확인
            boolean targetCreated = false;

            try (
                    OutputStream outputStream = Files.newOutputStream(
                            target,
                            // 파일 존재 확인과 생성을 한 번에 처리하여 기존 파일을 덮어쓰지 않게함
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                    )
            ) {
                targetCreated = true;

                try (InputStream inputStream = image.getInputStream()) {
                    inputStream.transferTo(outputStream);
                }

                return publicPath + storedFileName;
            } catch (FileAlreadyExistsException e) {
                // UUID가 우연히 중복되면 기존 파일을 유지하고 새 UUID로 재시도합니다.
                continue;
            } catch (IOException e) {
                if (targetCreated) {
                    deletePartiallyWrittenFile(target);
                }
                throw new IllegalStateException(imageLabel + " 저장에 실패했습니다.", e);
            }
        }

        throw new IllegalStateException(imageLabel + " 파일명을 생성하지 못했습니다.");
    }

    /** 공개 URL에서 파일명을 꺼내 해당 용도의 저장 폴더 안에 있는 파일만 삭제합니다. */
    public void delete(String storedPublicPath) {
        if (storedPublicPath == null || !storedPublicPath.startsWith(publicPath)) {
            return;
        }

        String storedFileName = Path.of(storedPublicPath).getFileName().toString();
        Path target = imageDirectory.resolve(storedFileName).normalize();

        if (!target.startsWith(imageDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target); // 파일이 있다면 삭
        } catch (IOException ignored) {
            // DB 처리의 원래 성공·실패 결과를 파일 정리 오류로 덮어쓰지 않습니다.
        }
    }


    //파일생성
    private void createImageDirectory() {
        try {
            Files.createDirectories(imageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(imageLabel + " 폴더 생성에 실패했습니다.", e);
        }
    }

    // 이미지 검증 + 인증된 확장자인지(ALLOWED_IMAGE_TYPES에 정의)
    private String validateAndGetExtension(MultipartFile image) {
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new InvalidInputException(validationField, tooLargeReason);
        }

        String extension = ALLOWED_IMAGE_TYPES.get(image.getContentType());
        if (extension == null || !hasReadableImageContent(image)) {
            throw new InvalidInputException(validationField, invalidReason);
        }
        return extension;
    }



    /** 브라우저가 보낸 MIME 문자열뿐 아니라 ImageIO가 실제 이미지로 읽을 수 있는지도 확인합니다.
     * 확장자만 .jpg, png 일수도 있기때문에
     * */
    private boolean hasReadableImageContent(MultipartFile image) {
        try (InputStream inputStream = image.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(inputStream); //BufferedImage -> ImageIO로 이미지 읽어서 정보 확인
            return bufferedImage != null
                    && bufferedImage.getWidth() > 0
                    && bufferedImage.getHeight() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void deletePartiallyWrittenFile(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 원래 저장 실패 예외를 유지합니다.
        }
    }
}