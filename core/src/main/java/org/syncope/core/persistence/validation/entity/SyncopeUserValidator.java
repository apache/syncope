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
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.policy.AccountPolicyEnforcer;
import org.syncope.core.policy.PasswordPolicyEnforcer;
import org.syncope.core.policy.PolicyEvaluator;
import org.syncope.types.AccountPolicySpec;
import org.syncope.types.EntityViolationType;
import org.syncope.types.PasswordPolicySpec;

public class SyncopeUserValidator extends AbstractValidator
        implements ConstraintValidator<SyncopeUserCheck, SyncopeUser> {

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyEvaluator evaluator;

    @Autowired
    private PasswordPolicyEnforcer ppEnforcer;

    @Autowired
    private AccountPolicyEnforcer apEnforcer;

    @Override
    public void initialize(final SyncopeUserCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final SyncopeUser object,
            final ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        // ------------------------------
        // Verify password policies
        // ------------------------------
        LOG.debug("Password Policy enforcement");

        try {
            for (Policy policy : getPasswordPolicies(object)) {
                // clearPassword must exist during creation/password update
                final String password = object.getClearPassword();

                // evaluate/enforce only during creation or password update
                if (password != null) {
                    // evaluate policy
                    final PasswordPolicySpec passwordPolicy =
                            evaluator.evaluate(policy, object);

                    // enforce policy
                    ppEnforcer.enforce(
                            passwordPolicy, policy.getType(), password);
                }
            }
        } catch (Exception e) {
            LOG.debug("Invalid password");

            context.buildConstraintViolationWithTemplate(
                    e.getMessage()).addNode(
                    EntityViolationType.InvalidPassword.toString()).
                    addConstraintViolation();

            return false;
        } finally {
            // password has been validated, let's remove its
            // clear version
            object.removeClearPassword();
        }
        // ------------------------------

        // ------------------------------
        // Verify account policies
        // ------------------------------
        LOG.debug("Account Policy enforcement");

        try {
            // missing username
            for (Policy policy : getAccountPolicies(object)) {
                // evaluate policy
                final AccountPolicySpec accountPolicy =
                        evaluator.evaluate(policy, object);

                // enforce policy
                apEnforcer.enforce(accountPolicy, policy.getType(), object);
            }
        } catch (Exception e) {
            LOG.debug("Invalid username");

            context.buildConstraintViolationWithTemplate(
                    e.getMessage()).addNode(
                    EntityViolationType.InvalidUsername.toString()).
                    addConstraintViolation();

            return false;
        }
        // ------------------------------

        return true;
    }

    private List<PasswordPolicy> getPasswordPolicies(final SyncopeUser user) {
        final List<PasswordPolicy> policies = new ArrayList<PasswordPolicy>();

        // Add global policy
        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        if (policy != null) {
            policies.add(policy);
        }

        // add resource policies
        for (ExternalResource resource : user.getExternalResources()) {
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

    private List<AccountPolicy> getAccountPolicies(final SyncopeUser user) {
        final List<AccountPolicy> policies = new ArrayList<AccountPolicy>();

        // Add global policy
        AccountPolicy policy = policyDAO.getGlobalAccountPolicy();
        if (policy != null) {
            policies.add(policy);
        }

        // add resource policies
        for (ExternalResource resource : user.getExternalResources()) {
            policy = resource.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add role policies
        for (SyncopeRole role : user.getRoles()) {
            policy = role.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }
}
