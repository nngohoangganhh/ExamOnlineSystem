package com.hrm.project_spring.dto.student;

import com.hrm.project_spring.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAllResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Gender gender;
    private LocalDate dataOfBirth;

}
