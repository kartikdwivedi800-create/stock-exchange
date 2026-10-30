package com.nexusexchange.user.config;

import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;

    private AdminSeeder seeder(String username, String password) {
        return new AdminSeeder(new AdminProperties(username, password), userRepository, encoder);
    }

    @Test
    void createsAdminWithBcryptPasswordAndAdminRole() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@nexus.local")).thenReturn(false);

        seeder("admin", "S3cret-pass").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getEmail()).isEqualTo("admin@nexus.local");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getPasswordHash()).isNotEqualTo("S3cret-pass").startsWith("$2");
        assertThat(encoder.matches("S3cret-pass", admin.getPasswordHash())).isTrue();
    }

    @Test
    void skipsWhenUserAlreadyExists() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        seeder("admin", "S3cret-pass").run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void runningTwiceCreatesOnlyOneAdmin() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false, true);
        when(userRepository.existsByEmailIgnoreCase("admin@nexus.local")).thenReturn(false);
        AdminSeeder seeder = seeder("admin", "S3cret-pass");

        seeder.run(null);
        seeder.run(null);

        verify(userRepository, org.mockito.Mockito.times(1)).save(any(User.class));
    }

    @Test
    void skipsWhenCredentialsMissing() {
        seeder("", "").run(null);
        seeder(null, null).run(null);
        seeder("admin", "").run(null);
        seeder("", "S3cret-pass").run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
    }

    @Test
    void propertiesToStringHidesPassword() {
        assertThat(new AdminProperties("admin", "S3cret-pass").toString()).doesNotContain("S3cret-pass");
    }
}
