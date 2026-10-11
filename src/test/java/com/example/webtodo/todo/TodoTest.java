package com.example.webtodo.todo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class TodoTest {

    @Test
    void 昨日が期限で未完了なら期限切れになる() {
        LocalDate today = LocalDate.of(2026, 10, 11);
        Todo todo = new Todo();
        todo.setDueDate(today.minusDays(1));
        todo.setDone(false);

        assertTrue(todo.isOverdue(today));
    }

    @Test
    void 今日が期限で未完了なら期限切れにならない() {
        LocalDate today = LocalDate.of(2026, 10, 11);
        Todo todo = new Todo();
        todo.setDueDate(today);
        todo.setDone(false);

        assertFalse(todo.isOverdue(today));
    }

    @Test
    void 昨日が期限で完了済みなら期限切れにならない() {
        LocalDate today = LocalDate.of(2026, 10, 11);
        Todo todo = new Todo();
        todo.setDueDate(today.minusDays(1));
        todo.setDone(true);

        assertFalse(todo.isOverdue(today));
    }

    @Test
    void 期限がnullで未完了なら期限切れにならない() {
        LocalDate today = LocalDate.of(2026, 10, 11);
        Todo todo = new Todo();
        todo.setDueDate(null);
        todo.setDone(false);

        assertFalse(todo.isOverdue(today));
    }
}
