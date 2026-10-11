package com.example.webtodo.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.webtodo.user.AppUser;
import com.example.webtodo.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
    TodoService.class,
    com.example.webtodo.TestcontainersConfiguration.class,
})
public class TodoServiceTest {

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 空白だけの説明はnullで保存される() {
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setName("テスト");
        user = userRepository.save(user);

        TodoForm form = new TodoForm();
        form.setTitle("テストTODO");
        form.setDescription("  ");

        todoService.create(form, user.getId());

        assertEquals(1L, todoRepository.count());
        assertNull(todoRepository.findAll().get(0).getDescription());
    }

    @Test
    void 他ユーザーのTODOは取得できない() {
        AppUser user1 = new AppUser();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setName("ユーザー1");
        user1 = userRepository.save(user1);

        AppUser user2 = new AppUser();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setName("ユーザー2");
        user2 = userRepository.save(user2);

        TodoForm form = new TodoForm();
        form.setTitle("テストTODO");
        form.setDescription("  ");

        todoService.create(form, user2.getId());

        Long todoId = todoRepository.findAll().get(0).getId();
        Long otherUserId = user1.getId();

        assertThrows(TodoNotFoundException.class, () ->
            todoService.get(todoId, otherUserId)
        );
    }

    @Test
    void 本人のTODOは取得できる() {
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setName("テスト");
        user = userRepository.save(user);

        TodoForm form = new TodoForm();
        form.setTitle("テストTODO");
        form.setDescription("  ");

        todoService.create(form, user.getId());

        Long todoId = todoRepository.findAll().get(0).getId();
        Long userId = user.getId();

        assertEquals("テストTODO", todoService.get(todoId, userId).getTitle());
    }

    @Test
    void 他ユーザーのTODOは削除できない() {
        AppUser user1 = new AppUser();
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setName("ユーザー1");
        user1 = userRepository.save(user1);

        AppUser user2 = new AppUser();
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setName("ユーザー2");
        user2 = userRepository.save(user2);

        TodoForm form = new TodoForm();
        form.setTitle("テストTODO");
        form.setDescription("  ");

        todoService.create(form, user2.getId());

        Long todoId = todoRepository.findAll().get(0).getId();
        Long otherUserId = user1.getId();

        assertThrows(TodoNotFoundException.class, () ->
            todoService.delete(todoId, otherUserId)
        );
        assertEquals(1L, todoRepository.count());
    }

    @Test
    void 本人のTODOは削除できる() {
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setName("テスト");
        user = userRepository.save(user);

        TodoForm form = new TodoForm();
        form.setTitle("テストTODO");
        form.setDescription("  ");

        todoService.create(form, user.getId());

        Long todoId = todoRepository.findAll().get(0).getId();
        Long userId = user.getId();

        todoService.delete(todoId, userId);
        assertEquals(0L, todoRepository.count());
    }
}
