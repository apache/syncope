/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class PolicyValidator extends AbstractValidator<PolicyCheck, Policy> {

    @Autowired
    private PolicyDAO policyDAO;

    @Override
    public boolean isValid(final Policy object, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (object.getSpecification() != null
                && ((object instanceof PasswordPolicy && !(object.getSpecification() instanceof PasswordPolicySpec))
                || ((object instanceof AccountPolicy && !(object.getSpecification() instanceof AccountPolicySpec)))
                || ((object instanceof SyncPolicy && !(object.getSpecification() instanceof SyncPolicySpec))))) {

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.valueOf("Invalid" + object.getClass().getSimpleName()),
                    "Invalid policy specification")).addNode("specification").
                    addConstraintViolation();

            return false;
        }

        return true;
    }
}
