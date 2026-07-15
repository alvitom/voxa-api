package com.voxa.api.validation;

import com.voxa.api.model.request.RegisterUserRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterUserRequest> {
    @Override
    public boolean isValid(RegisterUserRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean isMatches = Objects.equals(value.password(), value.confirmPassword());

        if (!isMatches) {
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "Password and confirm password do not match"
            ).addPropertyNode("confirmPassword").addConstraintViolation();
        }

        return isMatches;
    }
}
