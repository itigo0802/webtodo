package com.example.webtodo.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Setter
public class TodoForm {

    @NotBlank(message = "タイトルは必ず入力してください")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;

    @Size(max = 1000, message = "説明は1000文字以内で入力してください")
    private String description;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate dueDate;

    static TodoForm from(Todo todo) {
        TodoForm form = new TodoForm();
        form.setTitle(todo.getTitle());
        form.setDueDate(todo.getDueDate());
        form.setDescription(todo.getDescription());
        return form;
    }
}
