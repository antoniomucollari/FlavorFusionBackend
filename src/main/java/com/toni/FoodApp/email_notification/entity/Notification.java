package com.toni.FoodApp.email_notification.entity;


import com.toni.FoodApp.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String subject;
    @NotBlank(message = "recipient is reqired")
    private String recipient;
    @Lob
    private String body;
    private NotificationType type;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private boolean isHtml;
}