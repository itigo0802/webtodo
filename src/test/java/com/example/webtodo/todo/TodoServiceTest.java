package com.example.webtodo.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import com.example.webtodo.user.AppUser;
import com.example.webtodo.user.UserRepository;

import jakarta.persistence.EntityManager;

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

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void 他ユーザーのTODOは完了切り替えできない() {
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
            todoService.toggle(todoId, otherUserId)
        );
        assertFalse(todoRepository.findById(todoId).orElseThrow().isDone());
    }

    @Test
    void 本人のTODOは未完了から完了にできる() {
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

        todoService.toggle(todoId, userId);
        entityManager.flush();
        entityManager.clear();

        assertTrue(todoRepository.findById(todoId).orElseThrow().isDone());
    }

    @Test
    void 本人のTODOは完了から未完了に戻せる() {
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

        todoService.toggle(todoId, userId);
        entityManager.flush();
        entityManager.clear();
        assertTrue(todoRepository.findById(todoId).orElseThrow().isDone());

        todoService.toggle(todoId, userId);
        entityManager.flush();
        entityManager.clear();

        assertFalse(todoRepository.findById(todoId).orElseThrow().isDone());
    }

    @Test
    void 他ユーザーのTODOは編集できない() {
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

        TodoForm editForm = new TodoForm();
        editForm.setTitle("変更後タイトル");
        editForm.setDescription("変更後の説明");

        assertThrows(TodoNotFoundException.class, () ->
            todoService.update(todoId, editForm, otherUserId)
        );

        entityManager.flush();
        entityManager.clear();

        Todo todo = todoRepository.findById(todoId).orElseThrow();
        assertEquals("テストTODO", todo.getTitle());
        assertNull(todo.getDescription());
    }

    @Test
    void 本人のTODOは編集できる() {
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

        TodoForm editForm = new TodoForm();
        editForm.setTitle("変更後タイトル");
        editForm.setDescription("変更後の説明");
        editForm.setDueDate(LocalDate.of(2026, 11, 1));

        todoService.update(todoId, editForm, userId);
        entityManager.flush();
        entityManager.clear();

        Todo todo = todoRepository.findById(todoId).orElseThrow();
        assertEquals("変更後タイトル", todo.getTitle());
        assertEquals("変更後の説明", todo.getDescription());
        assertEquals(LocalDate.of(2026, 11, 1), todo.getDueDate());
    }

    @Test
    void 編集時に空白だけの説明はnullで保存される() {
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setName("テスト");
        user = userRepository.save(user);

        TodoForm form = new TodoForm();
        form.setTitle("テストタイトル");
        form.setDescription("元の説明");

        todoService.create(form, user.getId());

        Long todoId = todoRepository.findAll().get(0).getId();
        Long userId = user.getId();

        TodoForm editForm = new TodoForm();
        editForm.setTitle("変更後タイトル");
        editForm.setDescription("    ");
        editForm.setDueDate(LocalDate.of(2026, 11, 1));

        todoService.update(todoId, editForm, userId);
        entityManager.flush();
        entityManager.clear();

        Todo todo = todoRepository.findById(todoId).orElseThrow();
        assertEquals("変更後タイトル", todo.getTitle());
        assertNull(todo.getDescription());
        assertEquals(LocalDate.of(2026, 11, 1), todo.getDueDate());
    }

    @Test
    void 一覧には本人のTODOだけが含まれる() {
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

        TodoForm form1 = new TodoForm();
        form1.setTitle("ユーザー1のTODO");
        todoService.create(form1, user1.getId());

        TodoForm form2 = new TodoForm();
        form2.setTitle("ユーザー2のTODO");
        todoService.create(form2, user2.getId());

        var todos = todoService.list(user1.getId());

        assertEquals(1, todos.size());
        assertEquals("ユーザー1のTODO", todos.get(0).getTitle());
    }

    @Test
    void 一覧は期限日が早い順に並ぶ() {
        AppUser user = new AppUser();
        user.setEmail("user1@example.com");
        user.setPassword("password");
        user.setName("ユーザー");
        user = userRepository.save(user);

        TodoForm later = new TodoForm();
        later.setTitle("期限が遅いTODO");
        later.setDueDate(LocalDate.of(2026, 11, 10));
        todoService.create(later, user.getId());

        TodoForm earlier = new TodoForm();
        earlier.setTitle("期限が早いTODO");
        earlier.setDueDate(LocalDate.of(2026, 11, 1));
        todoService.create(earlier, user.getId());

        TodoForm middle = new TodoForm();
        middle.setTitle("期限が中間のTODO");
        middle.setDueDate(LocalDate.of(2026, 11, 5));
        todoService.create(middle, user.getId());

        var todos = todoService.list(user.getId());

        assertEquals(3, todos.size());
        assertEquals("期限が早いTODO", todos.get(0).getTitle());
        assertEquals("期限が中間のTODO", todos.get(1).getTitle());
        assertEquals("期限が遅いTODO", todos.get(2).getTitle());
    }

    @Test
    void 同じ期限日のTODOはIDの降順に並ぶ() {
        AppUser user = new AppUser();
        user.setEmail("user1@example.com");
        user.setPassword("password");
        user.setName("ユーザー");
        user = userRepository.save(user);

        TodoForm first = new TodoForm();
        first.setTitle("先に作ったTODO");
        first.setDueDate(LocalDate.of(2026, 11, 1));
        todoService.create(first, user.getId());

        TodoForm second = new TodoForm();
        second.setTitle("後に作ったTODO");
        second.setDueDate(LocalDate.of(2026, 11, 1));
        todoService.create(second, user.getId());

        var todos = todoService.list(user.getId());

        assertEquals(2, todos.size());
        assertEquals("後に作ったTODO", todos.get(0).getTitle());
        assertEquals("先に作ったTODO", todos.get(1).getTitle());
        assertTrue(todos.get(0).getId() > todos.get(1).getId());
    }

    @Test
    void 期限なしのTODOは最後に並ぶ() {
        AppUser user = new AppUser();
        user.setEmail("user1@example.com");
        user.setPassword("password");
        user.setName("ユーザー");
        user = userRepository.save(user);

        TodoForm dated = new TodoForm();
        dated.setTitle("期限ありのTODO");
        dated.setDueDate(LocalDate.of(2026, 11, 1));
        todoService.create(dated, user.getId());

        TodoForm undated = new TodoForm();
        undated.setTitle("期限なしのTODO");
        undated.setDueDate(null);
        todoService.create(undated, user.getId());

        var todos = todoService.list(user.getId());

        assertEquals(2, todos.size());
        assertEquals("期限ありのTODO", todos.get(0).getTitle());
        assertEquals("期限なしのTODO", todos.get(1).getTitle());
        assertNull(todos.get(1).getDueDate());
    }
}
