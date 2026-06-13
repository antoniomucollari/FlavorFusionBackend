package com.toni.FoodApp.email_notification.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.enums.NotificationType;
import com.toni.FoodApp.menu.entity.Menu;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.awt.*;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class NotificationDTO {
    private Long id;
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    private NotificationType type;
    private List<Menu> menus;
    private String recipient;
    private String subject;
    private String body;
    private boolean isHtml;
}
