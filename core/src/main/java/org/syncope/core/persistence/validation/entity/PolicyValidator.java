/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.types.EntityViolationType;

public class PolicyValidator extends AbstractValidator
        implements ConstraintValidator<PolicyCheck, Policy> {

    @Autowired
    private PolicyDAO policyDAO;

    @Override
    public void initialize(final PolicyCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(
            final Policy object,
            final ConstraintValidatorContext context) {

        boolean isValid = Boolean.TRUE;
        context.disableDefaultConstraintViolation();

        switch (object.getType()) {
            case PASSWORD:
                // just one policy with type PASSWORD
                Policy passwordPolicy = policyDAO.getPasswordPolicy();
                if (passwordPolicy != null
                        && !passwordPolicy.getId().equals(object.getId())) {
                    isValid = Boolean.FALSE;

                    context.buildConstraintViolationWithTemplate(
                            "Password policy already exists").addNode(
                            EntityViolationType.InvalidPolicy.toString()).
                            addConstraintViolation();
                }
                break;

            case ACCOUNT:
                // just one policy with type ACCOUNT
                Policy accountPolicy = policyDAO.getAccountPolicy();
                if (accountPolicy != null
                        && !accountPolicy.getId().equals(object.getId())) {
                    isValid = Boolean.FALSE;

                    context.buildConstraintViolationWithTemplate(
                            "Account policy already exists").
                            addNode(EntityViolationType.InvalidPolicy.toString()).
                            addConstraintViolation();
                }
                break;

            case SCHEMA:
            default:
        }

        return isValid;
    }
}
