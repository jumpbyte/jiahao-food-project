package com.jihao.food.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    private Long userId;

    private String username;

    private String realName;

    private List<String> roles;
}
