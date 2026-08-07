package kr.adapterz.Artifact.controller;

import jakarta.validation.Valid;
import kr.adapterz.Artifact.dto.user.*;
import kr.adapterz.Artifact.entity.User;

import kr.adapterz.Artifact.response.ApiResponse;
import kr.adapterz.Artifact.security.JwtTokenProvider;
import kr.adapterz.Artifact.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static kr.adapterz.Artifact.response.code.SuccessCode.*;

/**
 * 사용자 관련 HTTP 요청을 받는 클래스입니다.
 * Controller의 역할은 다음 세 가지입니다.
 * 1. URL, 헤더, JSON 및 멀티파트 요청을 Java 값으로 받습니다.
 * 2. 실제 규칙 처리는 UserService에 요청합니다.
 * 3. 처리 결과를 API 응답 DTO로 바꾸어 반환합니다.
 */

@RestController
public class UserController {
    // 회원가입, 로그인, 회원 수정 같은 실제 규칙은 Service가 담당합니다.
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    // 생성자 주입: Spring이 실행될 때 UserService 객체를 넣어 줍니다.
    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 회원가입 API: POST /users/signup
     * 회원이 새로 생성되었으므로 기본 200이 아니라 201 Created를 반환합니다.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            value = "/users/signup",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserSignupResponseDto> signup(
            // FormData의 request 파트는 application/json이며 DTO 검증도 그대로 적용됩니다.
            @Valid
            @RequestPart("request")
            UserSignupRequestDto request,
            // required = false 프로필 이미지는 선택 요청파트가 없어도 회원가입할 수 있다.
            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage
    ) {
        // Service가 입력값 검사, 선택 이미지 저장, 사용자 DB 저장을 순서대로 처리합니다.
        User user = userService.signup(request, profileImage);
        // 생성된 사용자 번호를 기존 API 명세 형식으로 반환합니다.
        return ApiResponse.of(USER_ADD_SUCCESS, new UserSignupResponseDto(user));
    }



    /**
     * 로그인 API: POST /users/login
     * @RequestBody는 요청 JSON을 UserLoginRequestDto로 변환합니다.
     * @Valid는 DTO의 이메일 형식과 빈 값 여부를 검사합니다.
     */
    @PostMapping("/users/login")
    public ApiResponse<UserLoginResponseDto> login(
            @Valid @RequestBody UserLoginRequestDto request
    ) {
        // 1. 이메일과 비밀번호 확인은 Service에 맡깁니다.
        User user = userService.login(request.getEmail(), request.getPassword());

        // 2. 비밀번호를 제외하고 로그인에 필요한 정보만 응답합니다.
        return ApiResponse.of(
                LOGIN_SUCCESS,
                new UserLoginResponseDto(
                        jwtTokenProvider.createToken(
                                user.getId(),
                                user.getTokenVersion()
                        ),
                        user.getId(),
                        user.getNickname(),
                        // DB의 상대 경로를 내려주고 실제 URL 조합은 프론트 공통 함수가 담당합니다.
                        user.getProfileImage(),
                        user.getRole()
                )
        );
    }

    /**
     * 탈퇴 계정 복구 API: POST /users/recovery
     * 이메일과 기존 비밀번호를 확인하고 복구된 사용자에게 새 버전의 JWT를 발급합니다.
     */
    @PostMapping("/users/recovery")
    public ApiResponse<UserLoginResponseDto> recover(
            @Valid @RequestBody UserRecoveryRequestDto request
    ) {
        User user = userService.recover(request);
        return ApiResponse.of(
                USER_RECOVERY_SUCCESS,
                new UserLoginResponseDto(
                        jwtTokenProvider.createToken(
                                user.getId(),
                                user.getTokenVersion()
                        ),
                        user.getId(),
                        user.getNickname(),
                        user.getProfileImage(),
                        user.getRole()
                )
        );
    }

    /**
     * 현재 로그인한 사용자 조회 API: GET /users/me
     * 프로필 수정 화면이 localStorage의 오래된 값이 아니라 DB의 최신 값을 사용하게 합니다.
     */
    @GetMapping("/users/me")
    public ApiResponse<UserResponseDto> getMyProfile(
            @AuthenticationPrincipal Long userId
    ) {
        // JWT에서 꺼낸 userId로 조회하므로 요청자가 다른 사용자의 정보를 지정할 수 없습니다.
        User user = userService.requireAuthenticated(userId);
        return ApiResponse.of(USER_ACCESS_SUCCESS, new UserResponseDto(user));
    }



    /**
     * 프로필 수정 API: PATCH /users/profile
     * @AuthenticationPrincipal 어노테이션 -> JWT 필터가 인증한 사용자 ID를 전달합니다.
     */
    @PatchMapping(
            value = "/users/profile",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<UserIdResponseDto> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequestDto request
    ) {
        // Service가 로그인 여부와 닉네임 중복을 확인한 후 프로필을 변경합니다.
        User user = userService.updateProfile(userId, request);
        return ApiResponse.of(USER_MODIFY_SUCCESS, new UserIdResponseDto(user.getId()));
    }

    /**
     * 이미지까지 수정하는 멀티파트 API입니다.
     * request 파트에는 닉네임·삭제 여부 JSON, profile_image에는 선택 파일이 들어옵니다.
     */
    @PatchMapping(
            value = "/users/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserProfileUpdateResponseDto> updateProfileWithImage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") UserProfileUpdateRequestDto request,
            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage
    ) {
        User user = userService.updateProfile(userId, request, profileImage);
        // 최신 경로를 응답해 프론트가 localStorage와 헤더를 즉시 동기화하게 합니다.
        return ApiResponse.of(
                USER_MODIFY_SUCCESS,
                new UserProfileUpdateResponseDto(user)
        );
    }



    /**
     * 회원 탈퇴 API: DELETE /users/profile
     * 탈퇴할 때 사용자가 작성한 게시글과 댓글도 Service에서 함께 정리합니다.
     */
    @DeleteMapping("/users/profile")
    public ApiResponse<Void> deleteProfile(
            @AuthenticationPrincipal Long userId
    ) {
        userService.delete(userId);

        // 삭제 응답에는 추가 데이터가 필요하지 않으므로 data는 null입니다.
        return ApiResponse.of(DELETE_USER_SUCCESS, null);
    }



    /** 비밀번호 수정 API: PATCH /users/profile/password */
    @PatchMapping("/users/profile/password")
    public ApiResponse<UserIdResponseDto> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordUpdateRequestDto request
    ) {
        // 로그인 여부, 비밀번호 형식, 비밀번호 확인 일치 여부를 검사한 뒤 변경합니다.
        userService.updatePassword(userId, request);
        return ApiResponse.of(PASSWORD_CHANGE_SUCCESS, new UserIdResponseDto(userId));
    }

}
