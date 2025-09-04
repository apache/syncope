package org.apache.syncope.core.persistence.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;

public class PasswordManagementValidator extends AbstractValidator<PasswordManagementCheck, PasswordManagement> {

    @Override
    public boolean isValid(final PasswordManagement passwordManagement, final ConstraintValidatorContext context) {
        PasswordManagementDAO passwordManagementDAO =
                ApplicationContextProvider.getApplicationContext().getBean(PasswordManagementDAO.class);
        context.disableDefaultConstraintViolation();

        if (!Boolean.parseBoolean(passwordManagement.getEnabled())) {
            return true;
        }

        boolean isValid = !passwordManagementDAO.isAnotherInstanceEnabled(passwordManagement.getKey());
        if (!isValid) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.MoreThanOneEnabled,
                            "Enable only one configuration")).
                    addPropertyNode("key").addConstraintViolation();
        }
        return isValid;
    }
}
