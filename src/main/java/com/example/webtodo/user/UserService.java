package com.example.webtodo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public void register(RegisterForm form) {
        String email = normalizeEmail(form.getEmail());
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(encoder.encode(form.getPassword()));
        user.setName(form.getName().trim());
        repository.save(user);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
