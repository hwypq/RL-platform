package org.example.rlplatform.anno;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.rlplatform.validation.EmailSuffixValidator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = { EmailSuffixValidator.class }
)
public @interface ValidEmailSuffix {

    String message() default "邮箱后缀不支持";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] allowedSuffixes() default {};
}
