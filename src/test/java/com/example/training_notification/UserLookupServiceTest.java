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
public class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Test
    @DisplayName("Should return user email when exists in database")
    void getEmailByUser_Success() {
        System.out.println("Running test: Should return email if user exists");

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("pro_athlete@gmail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String result = userLookupService.getEmailByUserId(userId);

        System.out.println("Resulting email: " + result);
        assertEquals("pro_athlete@gmail.com", result);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user is not found in database")
    void getEmailByUserId_NotFound() {
        System.out.println("Running test: Should throw exception if user is missing");
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userLookupService.getEmailByUserId(userId));

        System.out.println("Exception caught: " + exception.getMessage());
        assertEquals("User not found", exception.getMessage());
    }
}