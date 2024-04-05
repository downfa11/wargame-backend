package com.ns.wargame.Config;

import jakarta.validation.Constraint;
import jakarta.validation.constraints.Pattern;
import org.springframework.messaging.handler.annotation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = "^[a-zA-Z0-9가-힣]*$", message = "Must be alphanumeric and Korean characters only")
public @interface Numberic {
    String message() default "Must not contain spaces";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}