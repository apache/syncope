package org.apache.syncope.core.persistence.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordManagementValidator.class)
@Documented
public @interface PasswordManagementCheck {

    String message() default "{org.apache.syncope.core.persistence.validation.passwordManagement}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
