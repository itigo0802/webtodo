package com.example.webtodo.user;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("メールアドレスが既に登録されています");
    }
}
