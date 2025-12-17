package com.workflow.demo.dto;
import lombok.Data;

@Data
public class CreateUserDto {
    private String email;
    private String password;
    private String name;
}
