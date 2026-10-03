package com.example.webtodo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "メールアドレスは必ず入力してください")
    @Email(message = "メールアドレスの形式が正しくありません")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;

    @NotBlank(message = "パスワードは必ず入力してください")
    @Size(
        min = 8,
        max = 72,
        message = "パスワードは8文字以上72文字以下で入力してください"
    )
    private String password;

    @NotBlank(message = "ニックネームは必ず入力してください")
    @Size(max = 50, message = "ニックネームは50文字以内で入力してください")
    private String name;
}
