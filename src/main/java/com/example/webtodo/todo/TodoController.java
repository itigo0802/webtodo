package com.example.webtodo.todo;

import com.example.webtodo.user.AppUserPrincipal;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class TodoController {

    private final TodoService service;

    @GetMapping("/todos")
    public String list(
        Model model,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        model.addAttribute("todos", service.list(principal.getId()));
        return "todo/list";
    }

    @GetMapping("/todos/new")
    public String create(Model model) {
        model.addAttribute("todoForm", new TodoForm());
        return "todo/form";
    }

    @PostMapping("/todos/new")
    public String store(
        @Valid @ModelAttribute TodoForm form,
        BindingResult result,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        if (result.hasErrors()) {
            return "todo/form";
        }
        service.create(form, principal.getId());
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/toggle")
    public String toggle(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal,
        HtmxRequest htmxRequest,
        Model model
    ) {
        service.toggle(id, principal.getId());
        if (htmxRequest.isHtmxRequest()) {
            model.addAttribute("todo", service.get(id, principal.getId()));
            return "fragments/todoItem :: todoItem";
        }
        return "redirect:/todos";
    }

    @GetMapping("/todos/{id}/edit")
    public String edit(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal,
        Model model
    ) {
        model.addAttribute("todoId", id);
        model.addAttribute(
            "todoForm",
            TodoForm.from(service.get(id, principal.getId()))
        );
        return "todo/form";
    }

    @PostMapping("/todos/{id}/edit")
    public String update(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal,
        @Valid @ModelAttribute TodoForm form,
        BindingResult result,
        Model model
    ) {
        if (result.hasErrors()) {
            model.addAttribute("todoId", id);
            return "todo/form";
        }
        service.update(id, form, principal.getId());
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/delete")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal,
        HtmxRequest htmxRequest
    ) {
        service.delete(id, principal.getId());
        if (htmxRequest.isHtmxRequest()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .location(URI.create("/todos"))
            .build();
    }

    @ModelAttribute("today")
    LocalDate today() {
        return LocalDate.now();
    }
}
