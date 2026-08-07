package kr.adapterz.Artifact.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

import static kr.adapterz.Artifact.response.code.ValidationField.POST_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_TOO_LARGE;

/** 게시글 이미지를 uploads/posts 폴더에 저장하고 공개 URL 경로를 반환합니다. */
@Service
public class PostImageStorageService extends AbstractImageStorageService {
    @Autowired
    public PostImageStorageService(
            @Value("${app.upload.directory:uploads}") String uploadDirectory
    ) {
        this(uploadDirectory, UUID::randomUUID);
    }

    PostImageStorageService(
            String uploadDirectory,
            Supplier<UUID> uuidSupplier
    ) {
        super(
                uploadDirectory,
                "posts",
                "/uploads/posts/",
                POST_IMAGE,
                POST_IMAGE_INVALID,
                POST_IMAGE_TOO_LARGE,
                "게시글 이미지",
                uuidSupplier
        );
    }
}
