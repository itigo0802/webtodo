package com.example.webtodo.todo;

import com.example.webtodo.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Todo> list(Long userId) {
        return todoRepository.findByUserIdOrderByDueDateAscIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public Todo get(Long id, Long userId) {
        return todoRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(TodoNotFoundException::new);
    }

    public void create(TodoForm form, Long userId) {
        Todo todo = new Todo();
        todo.setUser(userRepository.getReferenceById(userId));
        todo.setTitle(form.getTitle());
        todo.setDueDate(form.getDueDate());
        todo.setDescription(normalizeDescriptionBlank(form.getDescription()));
        todoRepository.save(todo);
    }

    public void update(Long id, TodoForm form, Long userId) {
        Todo todo = todoRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(TodoNotFoundException::new);
        todo.setTitle(form.getTitle());
        todo.setDueDate(form.getDueDate());
        todo.setDescription(normalizeDescriptionBlank(form.getDescription()));
    }

    public void toggle(Long id, Long userId) {
        Todo todo = todoRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(TodoNotFoundException::new);
        todo.setDone(!todo.isDone());
    }

    public void delete(Long id, Long userId) {
        Todo todo = todoRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(TodoNotFoundException::new);
        todoRepository.delete(todo);
    }

    static String normalizeDescriptionBlank(String description) {
        return description == null || description.isBlank()
            ? null
            : description;
    }
}
