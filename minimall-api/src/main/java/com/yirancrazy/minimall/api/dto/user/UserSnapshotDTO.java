package com.yirancrazy.minimall.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshotDTO {
    private Long userId;
    private String username;
    private String role;
}