package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest extends IntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void storesOnlyABcryptHashAndAuthenticatesCaseInsensitively() {
        AppUser user = userService.createUser("directrice", "Mme Exemple", "secret123");

        assertThat(user.getPasswordHash()).startsWith("$2").doesNotContain("secret123");
        assertThat(userService.authenticate("Directrice", "secret123")).isPresent();
        assertThat(userService.authenticate("directrice", "wrong")).isEmpty();
        assertThat(userService.authenticate("nobody", "secret123")).isEmpty();
    }

    @Test
    void rejectsShortPasswordsAndDuplicateNames() {
        assertThatThrownBy(() -> userService.createUser("admin", null, "123")).hasMessage("validation.password.tooShort");
        userService.createUser("admin", null, "123456");
        assertThatThrownBy(() -> userService.createUser("ADMIN", null, "123456")).hasMessage("users.error.exists");
    }

    @Test
    void inactiveUsersCannotLogInAndTheLastActiveUserIsProtected() {
        AppUser first = userService.createUser("first", null, "123456");
        assertThatThrownBy(() -> userService.setActive(first.getId(), false)).hasMessage("users.error.lastActive");

        AppUser second = userService.createUser("second", null, "123456");
        userService.setActive(second.getId(), false);
        assertThat(userService.authenticate("second", "123456")).isEmpty();
    }

    @Test
    void changesThePassword() {
        AppUser user = userService.createUser("staff", null, "old-password");
        userService.changePassword(user.getId(), "new-password");
        assertThat(userService.authenticate("staff", "new-password")).isPresent();
        assertThat(userService.authenticate("staff", "old-password")).isEmpty();
    }
}
