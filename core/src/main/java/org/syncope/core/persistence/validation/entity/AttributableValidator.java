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

import java.util.ArrayList;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.policy.PasswordPolicyEnforcer;
import org.syncope.core.policy.PolicyEvaluator;
import org.syncope.types.EntityViolationType;
import org.syncope.types.PasswordPolicy;

public class AttributableValidator extends AbstractValidator
        implements ConstraintValidator<AttributableCheck, AbstractAttributable> {

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyEvaluator evaluator;

    @Autowired
    private PasswordPolicyEnforcer enforcer;

    @Override
    public void initialize(final AttributableCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(
            final AbstractAttributable object,
            final ConstraintValidatorContext context) {

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (object instanceof SyncopeUser) {
            // ------------------------------
            // Verify password policy
            // ------------------------------
            LOG.debug("Password Policy enforcement");

            LOG.error("AAAA");
            final List<Policy> policies =
                    getPasswordPolicies((SyncopeUser) object);

            try {
                for (Policy policy : policies) {
                    // clearPassword must exist during creation/password update
                    final String password =
                            ((SyncopeUser) object).getClearPassword();

                    // evaluate/enforce only during creation or password update
                    if (password != null) {
                        // evaluate policy
                        final PasswordPolicy passwordPolicy =
                                evaluator.evaluate(policy, object);

                        // enforce policy
                        enforcer.enforce(
                                passwordPolicy, policy.getType(), password);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Invalid password");

                context.buildConstraintViolationWithTemplate(
                        e.getMessage()).addNode(
                        EntityViolationType.InvalidPassword.toString()).
                        addConstraintViolation();
                isValid = false;
            } finally {
                // password has been validated, let's remove its
                // clear version
                ((SyncopeUser) object).removeClearPassword();
            }
            // ------------------------------
        }

        // Let's verify other policies ....
        return isValid;
    }

    private List<Policy> getPasswordPolicies(final SyncopeUser user) {
        final List<Policy> policies = new ArrayList<Policy>();

        // Add global policy
        Policy policy = policyDAO.getGlobalPasswordPolicy();

        if (policy != null) {
            policies.add(policy);
        }

        // add resource policies
        for (TargetResource resource : user.getTargetResources()) {
            policy = resource.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add role policies
        for (SyncopeRole role : user.getRoles()) {
            policy = role.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }
}
