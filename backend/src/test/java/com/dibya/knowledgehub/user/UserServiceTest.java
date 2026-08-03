package com.dibya.knowledgehub.user;

import com.dibya.knowledgehub.exception.ResourceNotFoundException;
import com.dibya.knowledgehub.role.Role;
import com.dibya.knowledgehub.user.dto.UpdateProfileRequest;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import com.dibya.knowledgehub.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getProfile_userNotFound_throwsResourceNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProfile_success_returnsProfileResponse() {
        var user = buildUser("alice@example.com", "Alice");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        var profile = userService.getProfile("alice@example.com");

        assertThat(profile.email()).isEqualTo("alice@example.com");
        assertThat(profile.name()).isEqualTo("Alice");
        assertThat(profile.provider()).isEqualTo("LOCAL");
        assertThat(profile.roles()).contains("ROLE_USER");
    }

    @Test
    void updateProfile_success_updatesNameAndEvictsCache() {
        var user = buildUser("bob@example.com", "Bob");
        var req = new UpdateProfileRequest("Bobby");

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var profile = userService.updateProfile("bob@example.com", req);

        assertThat(profile.name()).isEqualTo("Bobby");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_userNotFound_throwsResourceNotFound() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile("missing@example.com", new UpdateProfileRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User buildUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setEmailVerified(true);
        user.setRoles(Set.of(new Role("ROLE_USER")));
        return user;
    }
}
