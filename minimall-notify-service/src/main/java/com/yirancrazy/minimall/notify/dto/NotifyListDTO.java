package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Notify list query DTO.
 */
@Data
public class NotifyListDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}