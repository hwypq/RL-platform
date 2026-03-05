package org.example.rlplatform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.rlplatform.anno.ValidEmailSuffix;

import java.util.Arrays;
import java.util.List;


public class EmailSuffixValidator implements ConstraintValidator<ValidEmailSuffix, String> {
    private List<String> allowedSuffixes;

    @Override
    public void initialize(ValidEmailSuffix constraintAnnotation) {
        this.allowedSuffixes = Arrays.asList(constraintAnnotation.allowedSuffixes());
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true;  // 允许返回，由@NotEmpty字段注解处理
        } 

        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1) {
            return false;
        }

        String suffix = email.substring(atIndex + 1).toLowerCase();
        return allowedSuffixes.stream()
                .anyMatch(allowed -> suffix.equals(allowed.toLowerCase()));
    }
}
