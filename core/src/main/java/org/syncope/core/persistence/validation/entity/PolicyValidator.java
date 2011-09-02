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
import org.syncope.types.AccountPolicy;
import org.syncope.types.EntityViolationType;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.PolicyType;
import org.syncope.types.SyncPolicy;

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

        context.disableDefaultConstraintViolation();

        if (object.getSpecification() != null
                && (((object.getType() == PolicyType.PASSWORD
                || object.getType() == PolicyType.GLOBAL_PASSWORD)
                && !(object.getSpecification() instanceof PasswordPolicy))
                || ((object.getType() == PolicyType.ACCOUNT
                || object.getType() == PolicyType.GLOBAL_ACCOUNT)
                && !(object.getSpecification() instanceof AccountPolicy))
                || (object.getType() == PolicyType.SYNC
                && !(object.getSpecification() instanceof SyncPolicy)))) {

            context.buildConstraintViolationWithTemplate(
                    "Invalid password specification or password type").
                    addNode(
                    EntityViolationType.InvalidPolicy.toString()).
                    addConstraintViolation();

            return false;
        }

        switch (object.getType()) {
            case GLOBAL_PASSWORD:
                // just one policy with type PASSWORD
                Policy passwordPolicy = policyDAO.getGlobalPasswordPolicy();
                if (passwordPolicy != null
                        && !passwordPolicy.getId().equals(object.getId())) {

                    context.buildConstraintViolationWithTemplate(
                            "Password policy already exists").addNode(
                            EntityViolationType.InvalidPolicy.toString()).
                            addConstraintViolation();

                    return false;
                }
                break;
            case PASSWORD:
                break;

            case GLOBAL_ACCOUNT:

                // just one policy with type ACCOUNT
                Policy accountPolicy = policyDAO.getGlobalAccountPolicy();
                if (accountPolicy != null
                        && !accountPolicy.getId().equals(object.getId())) {

                    context.buildConstraintViolationWithTemplate(
                            "Account policy already exists").
                            addNode(EntityViolationType.InvalidPolicy.toString()).
                            addConstraintViolation();

                    return false;
                }
                break;

            case ACCOUNT:
                break;
                
            case SYNC:
            default:
        }

        return true;
    }
}
