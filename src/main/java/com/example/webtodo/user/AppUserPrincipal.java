package com.example.webtodo.user;

import lombok.Getter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

@Getter
public class AppUserPrincipal extends User {

    private final Long id;

    public AppUserPrincipal(AppUser user) {
        super(
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            true,
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        this.id = user.getId();
    }
}
