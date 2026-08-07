package kr.adapterz.Artifact.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

import static kr.adapterz.Artifact.response.code.ValidationField.PROFILE_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PROFILE_IMAGE_TOO_LARGE;

/** 프로필 이미지의 저장 폴더와 프로필 전용 검증 오류를 공통 저장 기능에 연결합니다. */
@Service
public class ProfileImageStorageService extends AbstractImageStorageService {
    @Autowired
    public ProfileImageStorageService(
            @Value("${app.upload.directory:uploads}") String uploadDirectory
    ) {
        this(uploadDirectory, UUID::randomUUID);
    }

    ProfileImageStorageService(
            String uploadDirectory,
            Supplier<UUID> uuidSupplier
    ) {
        super(
                uploadDirectory,
                "profiles",
                "/uploads/profiles/",
                PROFILE_IMAGE,
                PROFILE_IMAGE_INVALID,
                PROFILE_IMAGE_TOO_LARGE,
                "프로필 이미지",
                uuidSupplier
        );
    }
}
