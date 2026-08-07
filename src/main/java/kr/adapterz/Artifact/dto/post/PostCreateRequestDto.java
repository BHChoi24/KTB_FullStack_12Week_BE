package kr.adapterz.Artifact.dto.post;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.adapterz.Artifact.entity.PostCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static kr.adapterz.Artifact.response.code.ValidationMessage.*;

@Getter
@NoArgsConstructor
public class PostCreateRequestDto {

    // 작성자 번호는 body가 아니라 검증된 JWT에서 받습니다.
    @NotBlank(message = TITLE_EMPTY)
    @Size(max = 26, message = TITLE_INVALID)
    private String title;

    @NotBlank(message = CONTENT_EMPTY)
    private String content;

    @NotNull(message = CATEGORY_EMPTY)
    private PostCategory category;

    // 일반 글은 false이며 관리자 공지 작성 화면에서만 true를 전송합니다.
    private boolean pinned;

    // 이미지 파일은 JSON이 아니라 multipart의 post_image 파트로 별도 전달합니다.
}
