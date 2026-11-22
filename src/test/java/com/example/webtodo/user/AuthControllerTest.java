package com.example.webtodo.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.webtodo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void 登録POSTで入力が正しければログイン画面へリダイレクトされる() throws Exception {
        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("email", "user1@example.com")
                    .param("password", "password123")
                    .param("name", "ユーザー1")
            )
            .andExpect(redirectedUrl("/login"));

        ArgumentCaptor<RegisterForm> captor = ArgumentCaptor.forClass(
            RegisterForm.class
        );

        verify(userService).register(captor.capture());
        assertEquals("user1@example.com", captor.getValue().getEmail());
        assertEquals("password123", captor.getValue().getPassword());
        assertEquals("ユーザー1", captor.getValue().getName());
    }

    @Test
    void 登録POSTでパスワードが7文字なら登録画面に戻る() throws Exception {
        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("email", "user1@example.com")
                    .param("password", "1234567")
                    .param("name", "ユーザー1")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(
                model().attributeHasFieldErrorCode(
                    "registerForm",
                    "password",
                    "Size"
                )
            );

        verifyNoInteractions(userService);
    }
}
