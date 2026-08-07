package kr.adapterz.Artifact.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 존재하지_않는_정적_리소스는_500이_아니라_404를_반환한다() throws Exception {
        mockMvc.perform(get("/uploads/missing.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("resource_not_found"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @RestController
    private static class MissingResourceController {
        @GetMapping("/uploads/missing.png")
        void missingResource() throws NoResourceFoundException {
            // 실제 ResourceHttpRequestHandler가 없는 파일에서 발생시키는 예외와 동일합니다.
            throw new NoResourceFoundException(HttpMethod.GET, "uploads/missing.png");
        }
    }
}
