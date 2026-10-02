package com.kindererp.service;

import com.kindererp.model.AppUser;
import com.kindererp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.kindererp.service.Checks.*;

/** Application accounts. Passwords are only ever stored as BCrypt hashes. */
@Service
@RequiredArgsConstructor
public class UserService {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public Optional<AppUser> authenticate(String username, String password) {
        if (isBlank(username) || password == null) {
            return Optional.empty();
        }
        return repository.findByUsernameIgnoreCase(username.trim())
                .filter(AppUser::isActive)
                .filter(user -> encoder.matches(password, user.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return repository.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public AppUser createUser(String username, String fullName, String password) {
        require(username != null && username.trim().matches("[A-Za-z0-9._-]{3,50}"), "validation.username.invalid");
        require(!repository.existsByUsernameIgnoreCase(username.trim()), "users.error.exists", username.trim());
        checkPassword(password);

        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setFullName(trimToNull(fullName));
        user.setPasswordHash(encoder.encode(password));
        user.setActive(true);
        return repository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        checkPassword(newPassword);
        AppUser user = repository.findById(userId).orElseThrow(() -> new BusinessException("error.notFound"));
        user.setPasswordHash(encoder.encode(newPassword));
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        AppUser user = repository.findById(userId).orElseThrow(() -> new BusinessException("error.notFound"));
        if (!active && user.isActive()) {
            require(repository.countByActiveTrue() > 1, "users.error.lastActive");
        }
        user.setActive(active);
    }

    @Transactional
    public void delete(Long userId) {
        AppUser user = repository.findById(userId).orElseThrow(() -> new BusinessException("error.notFound"));
        if (user.isActive()) {
            require(repository.countByActiveTrue() > 1, "users.error.lastActive");
        }
        repository.delete(user);
    }

    private static void checkPassword(String password) {
        require(password != null && password.length() >= MIN_PASSWORD_LENGTH, "validation.password.tooShort", MIN_PASSWORD_LENGTH);
    }
}
