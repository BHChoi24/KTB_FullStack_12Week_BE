package kr.adapterz.Artifact.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * 탈퇴 전 공개 프로필 이미지를 HTTP에 노출되지 않는 복구 폴더에 격리하고,
 * 계정 복구 시 새로운 UUID 공개 파일로 되돌리는 파일 저장 서비스입니다.
 */
@Service
public class ProfileImageRecoveryStorageService {
    private static final Logger log =
            LoggerFactory.getLogger(ProfileImageRecoveryStorageService.class);
    private static final String PUBLIC_PROFILE_PATH = "/uploads/profiles/";
    private static final int MAX_UUID_ATTEMPTS = 5;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "png");

    private final Path publicProfileDirectory;
    private final Path recoveryProfileDirectory;
    private final Supplier<UUID> uuidSupplier;

    @Autowired
    public ProfileImageRecoveryStorageService(
            @Value("${app.upload.directory:uploads}") String uploadDirectory,
            @Value("${app.recovery.directory:recovery}") String recoveryDirectory
    ) {
        this(uploadDirectory, recoveryDirectory, UUID::randomUUID);
    }

    ProfileImageRecoveryStorageService(
            String uploadDirectory,
            String recoveryDirectory,
            Supplier<UUID> uuidSupplier
    ) {
        // 상대 경로 설정도 실행 위치 기준 절대 경로로 바꾸고 ".." 요소를 제거합니다.
        this.publicProfileDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve("profiles");
        this.recoveryProfileDirectory = Path.of(recoveryDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve("profiles");
        this.uuidSupplier = uuidSupplier;
    }

    /**
     * 공개 파일을 비공개 폴더에 먼저 복사합니다.
     * 공개 원본은 DB 커밋 후 별도로 삭제하므로 트랜잭션 롤백 시 계속 사용할 수 있습니다.
     */
    public String quarantine(String publicPath) {
        // DB에는 공개 URL이 저장되어 있으므로 실제 uploads/profiles 파일 경로로 안전하게 변환합니다.
        Path source = resolvePublicFile(publicPath);
        // 기본 프로필 사용자 또는 이미 파일이 유실된 사용자는 복구 키 없이 탈퇴할 수 있습니다.
        if (source == null || Files.notExists(source)) {
            return null;
        }

        String extension = extensionOf(source.getFileName().toString());
        createDirectory(recoveryProfileDirectory, "프로필 복구");

        for (int attempt = 0; attempt < MAX_UUID_ATTEMPTS; attempt++) {
            // 공개 URL을 복구 저장소에 그대로 재사용하지 않고 별도의 임의 파일 키를 만듭니다.
            String recoveryKey = uuidSupplier.get() + "." + extension;
            Path target = resolveInside(recoveryProfileDirectory, recoveryKey);
            try {
                // 대상이 이미 있으면 FileAlreadyExistsException이 발생하므로 기존 복구 파일을 덮어쓰지 않습니다.
                Files.copy(source, target);
                return recoveryKey;
            } catch (FileAlreadyExistsException e) {
                // 극히 드문 UUID 충돌은 새 UUID로 재시도합니다.
            } catch (IOException e) {
                throw new IllegalStateException("프로필 이미지를 복구 보관소에 저장하지 못했습니다.", e);
            }
        }
        throw new IllegalStateException("프로필 복구 파일명을 생성하지 못했습니다.");
    }

    /**
     * 비공개 복구 파일을 새 UUID 공개 파일로 복사하고 공개 상대 URL을 반환합니다.
     * 실제 복구 파일이 이미 없다면 기본 프로필을 사용하도록 null을 반환합니다.
     */
    public String restore(String recoveryKey) {
        // recoveryKey는 외부 URL이 아니라 서버 내부에서만 해석하는 파일명입니다.
        Path source = resolveRecoveryFile(recoveryKey);
        if (source == null || Files.notExists(source)) {
            return null;
        }

        String extension = extensionOf(source.getFileName().toString());
        createDirectory(publicProfileDirectory, "공개 프로필");

        for (int attempt = 0; attempt < MAX_UUID_ATTEMPTS; attempt++) {
            // 기존 브라우저 캐시와 탈퇴 전 URL을 분리하기 위해 복구 시에도 새 UUID를 발급합니다.
            String fileName = uuidSupplier.get() + "." + extension;
            Path target = resolveInside(publicProfileDirectory, fileName);
            try {
                Files.copy(source, target);
                return PUBLIC_PROFILE_PATH + fileName;
            } catch (FileAlreadyExistsException e) {
                // 공개 파일과 UUID가 충돌하면 기존 파일을 유지하고 다시 생성합니다.
            } catch (IOException e) {
                throw new IllegalStateException("복구 프로필 이미지를 공개 폴더에 저장하지 못했습니다.", e);
            }
        }
        throw new IllegalStateException("복구 프로필 공개 파일명을 생성하지 못했습니다.");
    }

    /** 탈퇴 또는 복구 트랜잭션이 완료된 뒤 더 이상 필요 없는 공개 파일을 정리합니다. */
    public void deletePublic(String publicPath) {
        deleteWithWarning(resolvePublicFile(publicPath), "공개 프로필");
    }

    /** 롤백되었거나 복구가 완료된 뒤 더 이상 필요 없는 비공개 파일을 정리합니다. */
    public void deleteRecovery(String recoveryKey) {
        deleteWithWarning(resolveRecoveryFile(recoveryKey), "복구 프로필");
    }

    /**
     * 복구 기간 만료 정리는 삭제 실패 시 계정 익명화를 중단해야 다음 실행에서 재시도할 수 있습니다.
     */
    public void deleteRecoveryStrict(String recoveryKey) {
        Path target = resolveRecoveryFile(recoveryKey);
        if (target == null) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("만료된 프로필 복구 파일 삭제 실패: {}", target, e);
            throw new IllegalStateException("만료된 프로필 복구 파일을 삭제하지 못했습니다.", e);
        }
    }

    /**
     * DB가 참조하지 않고 유예 시간도 지난 공개 프로필 파일을 정리합니다.
     * 트랜잭션 콜백이나 프로세스 종료로 즉시 삭제하지 못한 파일은 다음 실행에서 재시도됩니다.
     */
    public void deleteUnreferencedPublicFiles(
            Set<String> referencedPublicPaths,
            Duration gracePeriod
    ) {
        if (Files.notExists(publicProfileDirectory)) {
            return;
        }

        Instant deleteBefore = Instant.now().minus(gracePeriod);
        try (Stream<Path> files = Files.list(publicProfileDirectory)) {
            // 정규 파일 → DB 미참조 → 유예 시간 경과 순서로 좁힌 파일만 삭제합니다.
            files.filter(Files::isRegularFile)
                    .filter(path -> !referencedPublicPaths.contains(
                            PUBLIC_PROFILE_PATH + path.getFileName()
                    ))
                    .filter(path -> isOlderThan(path, deleteBefore))
                    .forEach(path ->
                            deleteWithWarning(path, "참조되지 않는 공개 프로필"));
        } catch (IOException e) {
            log.warn("공개 프로필 고아 파일 목록 조회 실패: {}", publicProfileDirectory, e);
        }
    }

    private Path resolvePublicFile(String publicPath) {
        // 다른 업로드 영역이나 임의 로컬 경로가 프로필 삭제 대상으로 들어오지 못하게 제한합니다.
        if (publicPath == null || !publicPath.startsWith(PUBLIC_PROFILE_PATH)) {
            return null;
        }
        return resolveInside(
                publicProfileDirectory,
                Path.of(publicPath).getFileName().toString()
        );
    }

    private Path resolveRecoveryFile(String recoveryKey) {
        if (recoveryKey == null || recoveryKey.isBlank()) {
            return null;
        }
        String fileName = Path.of(recoveryKey).getFileName().toString();
        // 폴더가 포함된 키는 거부하여 recovery/profiles 밖의 파일에 접근하지 못하게 합니다.
        if (!fileName.equals(recoveryKey)) {
            return null;
        }
        return resolveInside(recoveryProfileDirectory, fileName);
    }

    private Path resolveInside(Path directory, String fileName) {
        Path target = directory.resolve(fileName).normalize();
        // normalize 이후에도 대상이 지정된 저장 루트 내부인지 한 번 더 검사합니다.
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("허용되지 않은 이미지 저장 경로입니다.");
        }
        return target;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex < 0 ? "" : fileName.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalStateException("지원하지 않는 프로필 이미지 확장자입니다.");
        }
        return extension;
    }

    private void createDirectory(Path directory, String label) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(label + " 폴더를 만들지 못했습니다.", e);
        }
    }

    private void deleteWithWarning(Path target, String label) {
        if (target == null) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // DB 결과는 유지하되 운영자가 고아 파일을 확인할 수 있도록 원인과 경로를 기록합니다.
            log.warn("{} 파일 삭제 실패: {}", label, target, e);
        }
    }

    private boolean isOlderThan(Path path, Instant deleteBefore) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(deleteBefore);
        } catch (IOException e) {
            log.warn("프로필 파일 수정 시각 조회 실패: {}", path, e);
            return false;
        }
    }
}
