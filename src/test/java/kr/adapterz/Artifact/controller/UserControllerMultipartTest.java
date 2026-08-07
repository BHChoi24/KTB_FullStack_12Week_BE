package kr.adapterz.Artifact.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import kr.adapterz.Artifact.dto.user.UserSignupRequestDto;
import kr.adapterz.Artifact.dto.user.UserRecoveryRequestDto;
import kr.adapterz.Artifact.dto.user.UserProfileUpdateRequestDto;
import kr.adapterz.Artifact.entity.User;
import kr.adapterz.Artifact.security.JwtTokenProvider;
import kr.adapterz.Artifact.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프론트가 보내는 request JSON 파트와 profile_image 파일 파트가
 * UserController의 @RequestPart 인자에 올바르게 연결되는지 확인합니다.
 */
class UserControllerMultipartTest {

    private UserService userService;
    private JwtTokenProvider jwtTokenProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        UserController controller =
                new UserController(userService, jwtTokenProvider);

        // 운영 설정과 동일하게 password_check를 passwordCheck 필드로 변환합니다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(
                PropertyNamingStrategies.SNAKE_CASE
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    void 로그인_응답에_프로필_이미지_경로를_포함한다() throws Exception {
        User user = mock(User.class);
        given(user.getId()).willReturn(7L);
        given(user.getNickname()).willReturn("테스터");
        given(user.getProfileImage()).willReturn("/uploads/profiles/user.jpg");
        given(userService.login("user@test.com", "Password1!")).willReturn(user);
        given(jwtTokenProvider.createToken(7L, 0)).willReturn("access-token");

        mockMvc.perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "user@test.com",
                                          "password": "Password1!"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile_image")
                        .value("/uploads/profiles/user.jpg"));
    }

    @Test
    void 회원가입_JSON과_이미지_파트를_각각_DTO와_MultipartFile로_전달한다()
            throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "email": "user@test.com",
                  "password": "Password1!",
                  "password_check": "Password1!",
                  "nickname": "테스터"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "profile_image",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
        User savedUser = mock(User.class);

        given(savedUser.getId()).willReturn(7L);
        given(userService.signup(
                any(UserSignupRequestDto.class),
                any(MultipartFile.class)
        )).willReturn(savedUser);

        mockMvc.perform(
                        multipart("/users/signup")
                                .file(requestPart)
                                .file(imagePart)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("user_add_success"))
                .andExpect(jsonPath("$.data.user_id").value(7L));

        ArgumentCaptor<UserSignupRequestDto> requestCaptor =
                ArgumentCaptor.forClass(UserSignupRequestDto.class);
        ArgumentCaptor<MultipartFile> imageCaptor =
                ArgumentCaptor.forClass(MultipartFile.class);

        verify(userService).signup(
                requestCaptor.capture(),
                imageCaptor.capture()
        );

        assertEquals(
                "Password1!",
                requestCaptor.getValue().getPasswordCheck()
        );
        assertEquals(
                "profile.png",
                imageCaptor.getValue().getOriginalFilename()
        );
    }

    @Test
    void 프로필수정_JSON과_이미지_파트를_전달하고_최신_경로를_응답한다()
            throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "nickname": "새닉네임",
                  "remove_profile_image": false
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile imagePart = new MockMultipartFile(
                "profile_image",
                "new-profile.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
        User updatedUser = mock(User.class);
        given(updatedUser.getId()).willReturn(7L);
        given(updatedUser.getNickname()).willReturn("새닉네임");
        given(updatedUser.getProfileImage())
                .willReturn("/uploads/profiles/new-profile.png");
        given(userService.updateProfile(
                any(),
                any(UserProfileUpdateRequestDto.class),
                any(MultipartFile.class)
        )).willReturn(updatedUser);

        mockMvc.perform(
                        multipart("/users/profile")
                                .file(requestPart)
                                .file(imagePart)
                                .with(request -> {
                                    request.setMethod("PATCH");
                                    request.setUserPrincipal(() -> "7");
                                    return request;
                                })
                                .requestAttr(
                                        "org.springframework.security.core.annotation.AuthenticationPrincipal",
                                        7L
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user_modify_success"))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.profile_image")
                        .value("/uploads/profiles/new-profile.png"));
    }

    @Test
    void 탈퇴계정_복구에_성공하면_새_JWT와_프로필_경로를_반환한다()
            throws Exception {
        User recoveredUser = mock(User.class);
        given(recoveredUser.getId()).willReturn(7L);
        given(recoveredUser.getTokenVersion()).willReturn(2);
        given(recoveredUser.getNickname()).willReturn("복구사용자");
        given(recoveredUser.getProfileImage())
                .willReturn("/uploads/profiles/restored.png");
        given(userService.recover(any(UserRecoveryRequestDto.class)))
                .willReturn(recoveredUser);
        given(jwtTokenProvider.createToken(7L, 2))
                .willReturn("new-access-token");

        mockMvc.perform(
                        post("/users/recovery")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "user@test.com",
                                          "password": "Password1!"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("user_recovery_success"))
                .andExpect(jsonPath("$.data.token")
                        .value("new-access-token"))
                .andExpect(jsonPath("$.data.profile_image")
                        .value("/uploads/profiles/restored.png"));
    }
}
