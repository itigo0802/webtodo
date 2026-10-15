package com.example.webtodo.todo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import com.example.webtodo.config.SecurityConfig;
import com.example.webtodo.user.AppUser;
import com.example.webtodo.user.AppUserPrincipal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TodoController.class)
@Import(SecurityConfig.class)
public class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService todoService;

    @Test
    void 未ログインで一覧にアクセスするとログイン画面へ転送される()
        throws Exception {
        mockMvc
            .perform(get("/todos"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 他ユーザーのTODOの編集画面は404になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        when(todoService.get(99L, 1L))
            .thenThrow(new TodoNotFoundException());

        mockMvc.perform(
            get("/todos/99/edit").with(user(principal))
        )
        .andExpect(status().isNotFound());

        verify(todoService).get(99L, 1L);
    }

    @Test
    void 本人のTODOの編集画面を表示できる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        Todo todo = new Todo();
        todo.setUser(appUser);
        todo.setTitle("編集前のタイトル");
        todo.setDescription("編集前の説明");

        when(todoService.get(99L, 1L))
            .thenReturn(todo);

        mockMvc.perform(
            get("/todos/99/edit").with(user(principal))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("todo/form"))
        .andExpect(model().attribute("todoId", 99L))
        .andExpect(model().attribute(
            "todoForm",
            hasProperty("title", is("編集前のタイトル"))
        ));

        verify(todoService).get(99L, 1L);
    }
}
