package vn.ueh.kltn.restaurant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = Utf8SizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8Size {
    String message() default "Mật khẩu không được vượt quá 72 byte UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max();
}
