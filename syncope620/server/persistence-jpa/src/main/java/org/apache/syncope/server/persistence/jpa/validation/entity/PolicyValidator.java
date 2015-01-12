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
package org.apache.syncope.server.persistence.jpa.validation.entity;

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.server.persistence.api.entity.AccountPolicy;
import org.apache.syncope.server.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.server.persistence.api.entity.Policy;
import org.apache.syncope.server.persistence.api.entity.SyncPolicy;

public class PolicyValidator extends AbstractValidator<PolicyCheck, Policy> {

    @Override
    public boolean isValid(final Policy object, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        EntityViolationType violationType =
                object instanceof PasswordPolicy
                && !(object.getSpecification(PasswordPolicySpec.class) instanceof PasswordPolicySpec)
                        ? EntityViolationType.InvalidPasswordPolicy
                        : object instanceof AccountPolicy
                        && !(object.getSpecification(AccountPolicySpec.class) instanceof AccountPolicySpec)
                                ? EntityViolationType.InvalidAccountPolicy
                                : object instanceof SyncPolicy
                                && !(object.getSpecification(SyncPolicySpec.class) instanceof SyncPolicySpec)
                                        ? EntityViolationType.InvalidSyncPolicy
                                        : null;

        if (violationType != null) {
            context.buildConstraintViolationWithTemplate(getTemplate(violationType,
                    "Invalid policy specification")).addPropertyNode("specification").
                    addConstraintViolation();

            return false;
        }

        return true;
    }
}
