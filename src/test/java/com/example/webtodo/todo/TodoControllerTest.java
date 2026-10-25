package com.example.webtodo.todo;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

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

        when(todoService.get(99L, 1L)).thenThrow(new TodoNotFoundException());

        mockMvc
            .perform(get("/todos/99/edit").with(user(principal)))
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

        when(todoService.get(99L, 1L)).thenReturn(todo);

        mockMvc
            .perform(get("/todos/99/edit").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(view().name("todo/form"))
            .andExpect(model().attribute("todoId", 99L))
            .andExpect(
                model().attribute(
                    "todoForm",
                    hasProperty("title", is("編集前のタイトル"))
                )
            );

        verify(todoService).get(99L, 1L);
    }

    @Test
    void 他ユーザーのTODOへの編集POSTは404になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        doThrow(new TodoNotFoundException())
            .when(todoService)
            .update(eq(99L), any(TodoForm.class), eq(1L));

        mockMvc
            .perform(
                post("/todos/99/edit")
                    .with(user(principal))
                    .with(csrf())
                    .param("title", "変更後のタイトル")
            )
            .andExpect(status().isNotFound());

        verify(todoService).update(eq(99L), any(TodoForm.class), eq(1L));
    }

    @Test
    void 本人のTODOへの編集POSTは一覧へリダイレクトされる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/edit")
                    .with(user(principal))
                    .with(csrf())
                    .param("title", "変更後のタイトル")
            )
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/todos"));

        verify(todoService).update(eq(99L), any(TodoForm.class), eq(1L));
    }

    @Test
    void 編集POSTでタイトルが空白なら編集画面に戻る() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/edit")
                    .with(user(principal))
                    .with(csrf())
                    .param("title", "   ")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("todo/form"))
            .andExpect(model().attributeHasFieldErrors("todoForm", "title"))
            .andExpect(model().attribute("todoId", 99L));

        verifyNoInteractions(todoService);
    }

    @Test
    void 他ユーザーのTODOへの削除POSTは404になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        doThrow(new TodoNotFoundException()).when(todoService).delete(99L, 1L);

        mockMvc
            .perform(
                post("/todos/99/delete").with(user(principal)).with(csrf())
            )
            .andExpect(status().isNotFound());

        verify(todoService).delete(99L, 1L);
    }

    @Test
    void 本人のTODOへの削除POSTは一覧へリダイレクトされる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/delete").with(user(principal)).with(csrf())
            )
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/todos"));

        verify(todoService).delete(99L, 1L);
    }

    @Test
    void htmxの削除POSTは本文なしで200を返す() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/delete")
                    .with(user(principal))
                    .with(csrf())
                    .header("HX-Request", true)
            )
            .andExpect(status().isOk())
            .andExpect(content().string(""))
            .andExpect(header().doesNotExist("Location"));

        verify(todoService).delete(99L, 1L);
    }

    @Test
    void 他ユーザーのTODOへの完了切替POSTは404になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        doThrow(new TodoNotFoundException()).when(todoService).toggle(99L, 1L);

        mockMvc
            .perform(
                post("/todos/99/toggle").with(user(principal)).with(csrf())
            )
            .andExpect(status().isNotFound());

        verify(todoService).toggle(99L, 1L);
    }

    @Test
    void 本人のTODOへの完了切替POSTは一覧へリダイレクトされる()
        throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/toggle").with(user(principal)).with(csrf())
            )
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/todos"));

        verify(todoService).toggle(99L, 1L);
    }

    @Test
    void htmxの完了切替は更新後の行フラグメントを返す() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", 99L);
        todo.setUser(appUser);
        todo.setTitle("完了切替のテスト");
        todo.setDone(true);

        when(todoService.get(99L, 1L)).thenReturn(todo);

        mockMvc
            .perform(
                post("/todos/99/toggle")
                    .with(user(principal))
                    .with(csrf())
                    .header("HX-Request", "true")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("fragments/todoItem :: todoItem"))
            .andExpect(model().attribute("todo", todo))
            .andExpect(xpath("//tr/td/s").string("完了切替のテスト"));

        verify(todoService).toggle(99L, 1L);
        verify(todoService).get(99L, 1L);
    }

    @Test
    void htmxの完了切替で未完了の行フラグメントを返す() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", 99L);
        todo.setUser(appUser);
        todo.setTitle("完了切替のテスト");
        todo.setDone(false);

        when(todoService.get(99L, 1L)).thenReturn(todo);

        mockMvc
            .perform(
                post("/todos/99/toggle")
                    .with(user(principal))
                    .with(csrf())
                    .header("HX-Request", "true")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("fragments/todoItem :: todoItem"))
            .andExpect(model().attribute("todo", todo))
            .andExpect(xpath("//tr/td/span").string("完了切替のテスト"))
            .andExpect(xpath("//tr/td/s").doesNotExist());

        verify(todoService).toggle(99L, 1L);
        verify(todoService).get(99L, 1L);
    }

    @Test
    void CSRFトークンなしの完了切替POSTは403になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(post("/todos/99/toggle").with(user(principal)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(todoService);
    }

    @Test
    void CSRFトークンなしの削除POSTは403になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(post("/todos/99/delete").with(user(principal)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(todoService);
    }

    @Test
    void CSRFトークンなしの編集POSTは403になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/99/edit")
                    .with(user(principal))
                    .param("title", "変更後のタイトル")
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(todoService);
    }

    @Test
    void CSRFトークンなしの追加POSTは403になる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/new")
                    .with(user(principal))
                    .param("title", "変更後のタイトル")
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(todoService);
    }

    @Test
    void 本人のTODOへの追加POSTは一覧へリダイレクトされる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/new")
                    .with(user(principal))
                    .with(csrf())
                    .param("title", "変更後のタイトル")
            )
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/todos"));

        verify(todoService).create(any(TodoForm.class), eq(1L));
    }

    @Test
    void 追加POSTでタイトルが空白なら追加画面に戻る() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(
                post("/todos/new")
                    .with(user(principal))
                    .with(csrf())
                    .param("title", "　　　")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("todo/form"))
            .andExpect(model().attributeHasFieldErrors("todoForm", "title"));

        verifyNoInteractions(todoService);
    }

    @Test
    void ログイン済みなら追加画面を表示できる() throws Exception {
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "id", 1L);
        appUser.setEmail("user1@example.com");
        appUser.setPassword("unused");
        appUser.setName("ユーザー1");

        AppUserPrincipal principal = new AppUserPrincipal(appUser);

        mockMvc
            .perform(get("/todos/new").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(view().name("todo/form"))
            .andExpect(model().attributeExists("todoForm"))
            .andExpect(model().attributeHasNoErrors("todoForm"));

        verifyNoInteractions(todoService);
    }
}
