package kr.adapterz.Artifact.service;


import kr.adapterz.Artifact.dto.post.PostCreateRequestDto;
import kr.adapterz.Artifact.dto.post.PostUpdateRequestDto;
import kr.adapterz.Artifact.entity.Post;
import kr.adapterz.Artifact.entity.PostCategory;
import kr.adapterz.Artifact.entity.PostSort;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.entity.UserRole;
import kr.adapterz.Artifact.exception.ForbiddenException;
import kr.adapterz.Artifact.exception.InvalidInputException;
import kr.adapterz.Artifact.exception.InvalidPageException;
import kr.adapterz.Artifact.exception.NotFoundException;
import kr.adapterz.Artifact.repository.CommentRepository;
import kr.adapterz.Artifact.repository.PostFullTextRepository;
import kr.adapterz.Artifact.repository.PostLikeRepository;
import kr.adapterz.Artifact.repository.PostRepository;
import kr.adapterz.Artifact.repository.PostSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static kr.adapterz.Artifact.response.code.ErrorCode.POSTS_NOT_FOUND;
import static kr.adapterz.Artifact.response.code.ErrorCode.FORBIDDEN_NOTICE_MANAGEMENT;
import static kr.adapterz.Artifact.response.code.ValidationField.CONTENT;
import static kr.adapterz.Artifact.response.code.ValidationField.PINNED;
import static kr.adapterz.Artifact.response.code.ValidationField.POST_IMAGE;
import static kr.adapterz.Artifact.response.code.ValidationField.TITLE;
import static kr.adapterz.Artifact.response.code.ValidationMessage.CONTENT_EMPTY;
import static kr.adapterz.Artifact.response.code.ValidationMessage.POST_IMAGE_INVALID;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PINNED_LIMIT_EXCEEDED;
import static kr.adapterz.Artifact.response.code.ValidationMessage.PINNED_NOTICE_ONLY;
import static kr.adapterz.Artifact.response.code.ValidationMessage.TITLE_EMPTY;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final PostFullTextRepository postFullTextRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final PostImageStorageService postImageStorageService;

    public PostService(
            PostRepository postRepository,
            PostFullTextRepository postFullTextRepository,
            CommentRepository commentRepository,
            PostLikeRepository postLikeRepository,
            UserService userService,
            PostImageStorageService postImageStorageService
    ) {
        this.postRepository = postRepository;
        this.postFullTextRepository = postFullTextRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.userService = userService;
        this.postImageStorageService = postImageStorageService;
    }

    //게시물 생성
    @Transactional
    public Post createPost(
            Long userId,
            PostCreateRequestDto request,
            MultipartFile postImage
    ) {
        // 1. 로그인한 사용자인지 확인하고 user에 담기
        User user = userService.requireAuthenticated(userId);
        PostCategory category = request.getCategory();
        validateNoticeManagement(user, category, request.isPinned());
        if (request.isPinned()) ensurePinnedSlotAvailable();

        // 권한과 고정 개수 검증 뒤 파일을 저장해야 실패 요청이 불필요한 파일을 만들지 않습니다.
        String imageUrl = postImageStorageService.store(postImage);

        try {
            Post post = new Post(
                    user,
                    request.getTitle(),
                    request.getContent(),
                    imageUrl,
                    category
            );
            if (request.isPinned()) post.updatePinned(true);
            post = postRepository.saveAndFlush(post);
            // flush 이후 트랜잭션이 롤백되면 이번 요청이 만든 파일을 제거합니다.
            registerImageCleanupAfterCompletion(null, imageUrl);
            return post;
        } catch (RuntimeException e) {
            // DB 저장 단계에서 즉시 실패하면 콜백 등록 전이므로 새 파일을 바로 정리합니다.
            postImageStorageService.delete(imageUrl);
            throw e;
        }
    }

    //작성자 찾기
    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(POSTS_NOT_FOUND));
    }

    // 검색어 유무에 따라 FULLTEXT 또는 기존 목록 조회를 선택한다.
    public Page<Post> findPage(int page, int size, PostSearchCondition condition) {
        // 페이지 번호는 1부터 시작합니다.
        if (page < 1) throw new InvalidPageException();

        PageRequest pageable = PageRequest.of(page - 1, size, createSort(condition.sort()));
        if (condition.hasFullTextQuery()) {
            // 검색어가 있으면 MySQL FULLTEXT 조회를 사용한다.
            return postFullTextRepository.search(condition, pageable);
        }
        // 상단 고정 영역은 바로가기이므로 일반 목록에서는 pinned 여부와 관계없이 조건에 맞는 글을 조회합니다.
        Page<Post> postPage = postRepository.findAll(
                PostSpecifications.matches(condition),
                pageable
        );

        // 게시글이 존재하지만 요청한 페이지가 비어 있으면 잘못된 페이지입니다.
        if (postPage.isEmpty() && postPage.getTotalElements() > 0) {
            throw new InvalidPageException();
        }

        // 중요: 목록뿐 아니라 hasNext()도 상위 계층에서 사용할 수 있도록 Page 전체를 반환합니다.
        return postPage;
    }

    private Sort createSort(PostSort postSort) {
        return switch (postSort) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case LIKES -> Sort.by(Sort.Direction.DESC, "likesCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case VIEWS -> Sort.by(Sort.Direction.DESC, "viewsCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    /** 모든 카테고리 탭 상단에 공통으로 표시할 최신 고정 공지 두 개입니다. */
    public java.util.List<Post> findPinnedNotices() {
        return postRepository.findTop2ByPinnedTrueOrderByPinnedAtDescIdDesc();
    }

    //조회수 증가
    @Transactional
    public Post findByIdAndIncreaseView(Long postId) {
        Post post = findById(postId);
        post.increaseViewsCount();
        return post;
    }

    //게시물 업데이트
    @PreAuthorize("hasRole('ADMIN') or @postAuthorization.isAuthor(#postId, authentication.principal)")
    @Transactional
    public Post updatePost(
            Long userId,
            Long postId,
            PostUpdateRequestDto request,
            MultipartFile postImage
    ) {
        // 게시글 존재 여부 확인
        Post post = findById(postId);
        User user = userService.requireAuthenticated(userId);

        PostCategory targetCategory = request.getCategory() != null
                ? request.getCategory()
                : post.getCategory();
        boolean targetPinned = request.getPinned() != null
                ? request.getPinned()
                // 고정 공지를 일반 카테고리로 바꾸면 별도 pinned=false 없이 자동 해제합니다.
                : targetCategory == PostCategory.NOTICE && post.isPinned();
        // 화면에서 관리자 입력을 숨겨도 직접 API 요청은 가능하므로 서버가 권한을 다시 확인합니다.
        if (post.getCategory() == PostCategory.NOTICE
                && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException(FORBIDDEN_NOTICE_MANAGEMENT);
        }
        validateNoticeManagement(user, targetCategory, targetPinned);
        if (request.getPinned() != null
                && request.getPinned()
                && !post.isPinned()) {
            ensurePinnedSlotAvailable();
        }

        // PATCH에서 null은 미전달, 빈 문자열은 잘못된 입력입니다.
        if (request.getTitle() != null && request.getTitle().isBlank()) {
            throw new InvalidInputException(TITLE, TITLE_EMPTY);
        }
        if (request.getContent() != null && request.getContent().isBlank()) {
            throw new InvalidInputException(CONTENT, CONTENT_EMPTY);
        }
        boolean hasNewImage = postImage != null && !postImage.isEmpty();
        if (hasNewImage && request.isRemoveImage()) {
            // 새 파일 추가와 이미지 삭제를 동시에 요청하면 의도가 충돌하므로 거부합니다.
            throw new InvalidInputException(POST_IMAGE, POST_IMAGE_INVALID);
        }

        String previousImageUrl = post.getImageUrl();
        String newImageUrl = hasNewImage
                ? postImageStorageService.store(postImage)
                : null;

        try {
            post.updateText(request.getTitle(), request.getContent());
            if (request.getCategory() != null) {
                post.updateCategory(request.getCategory());
            }
            if (request.getPinned() != null) {
                post.updatePinned(request.getPinned());
            }

            // 새 파일은 교체, remove_image=true는 삭제, 둘 다 아니면 기존 이미지를 유지합니다.
            boolean imageChanged = hasNewImage || request.isRemoveImage();
            if (imageChanged) {
                post.updateImage(hasNewImage ? newImageUrl : null);
            }

            Post savedPost = postRepository.saveAndFlush(post);
            if (imageChanged) {
                registerImageCleanupAfterCompletion(previousImageUrl, newImageUrl);
            }
            return savedPost;
        } catch (RuntimeException e) {
            // DB 반영에 실패하면 기존 파일은 유지하고 새로 저장한 파일만 제거합니다.
            postImageStorageService.delete(newImageUrl);
            throw e;
        }
    }

    /** 공지 작성·변경·고정은 일반 작성자 권한과 별개인 관리자 전용 규칙입니다. */
    private void validateNoticeManagement(
            User user,
            PostCategory category,
            boolean pinned
    ) {
        if (pinned && category != PostCategory.NOTICE) {
            throw new InvalidInputException(PINNED, PINNED_NOTICE_ONLY);
        }
        if ((category == PostCategory.NOTICE || pinned)
                && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException(FORBIDDEN_NOTICE_MANAGEMENT);
        }
    }

    /** 자동 해제 대신 관리자에게 기존 고정 공지를 먼저 선택하도록 명확한 오류를 반환합니다. */
    private void ensurePinnedSlotAvailable() {
        if (postRepository.countByPinnedTrue() >= 2) {
            throw new InvalidInputException(PINNED, PINNED_LIMIT_EXCEEDED);
        }
    }

    //게시글삭제
    @PreAuthorize("hasRole('ADMIN') or @postAuthorization.isAuthor(#postId, authentication.principal)")
    @Transactional
    public void deletePost(Long postId) {
        // 게시글 존재 여부 확인
        Post post = findById(postId);

        // 게시글 아래 댓글과 좋아요를 먼저 삭제한 후 게시글을 삭제합니다.
        commentRepository.deleteAllByPostId(postId);
        postLikeRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
        postRepository.flush();

        // 게시글 DB 삭제가 커밋된 뒤에만 첨부 이미지 파일을 제거합니다.
        registerImageCleanupAfterCompletion(post.getImageUrl(), null);
    }

    /**
     * 커밋 성공 시 교체 전 파일을 삭제하고, 롤백 시 새로 만든 파일을 삭제합니다.
     * DB와 파일 시스템을 하나의 트랜잭션으로 묶을 수 없는 점을 보완하는 처리입니다.
     */
    private void registerImageCleanupAfterCompletion(
            String previousImageUrl,
            String newImageUrl
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 단위 테스트처럼 트랜잭션 밖이면 DB 작업이 끝난 시점에 기존 파일만 정리합니다.
            postImageStorageService.delete(previousImageUrl);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            postImageStorageService.delete(previousImageUrl);
                        } else {
                            postImageStorageService.delete(newImageUrl);
                        }
                    }
                }
        );
    }

}
