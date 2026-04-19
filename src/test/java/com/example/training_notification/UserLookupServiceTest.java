package com.example.training_notification;

import com.example.training_notification.entity.User;
import com.example.training_notification.repository.UserRepository;
import com.example.training_notification.service.impl.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Test
    @DisplayName("Should return the user email when the user exists")
    void getEmailByUserId_success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("pro_athlete@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String result = userLookupService.getEmailByUserId(userId);

        assertEquals("pro_athlete@gmail.com", result);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw when the user does not exist")
    void getEmailByUserId_notFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userLookupService.getEmailByUserId(userId)
        );

        assertEquals("User not found with ID: " + userId, exception.getMessage());
    }
}
