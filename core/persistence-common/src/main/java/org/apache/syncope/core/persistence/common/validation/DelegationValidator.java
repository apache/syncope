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
package org.apache.syncope.core.persistence.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Delegation;

public class DelegationValidator extends AbstractValidator<DelegationCheck, Delegation> {

    @Override
    public boolean isValid(final Delegation delegation, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (delegation.getDelegating().equals(delegation.getDelegated())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.Standard, "delegating must be different from delegated")).
                    addPropertyNode("delegating").addConstraintViolation();

            isValid = false;
        }

        if (isValid && delegation.getEnd() != null && !delegation.getEnd().isAfter(delegation.getStart())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.Standard, "when end is provided it must to be after start")).
                    addPropertyNode("end").addConstraintViolation();

            isValid = false;
        }

        if (isValid && !delegation.getDelegating().getRoles().containsAll(delegation.getRoles())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.Standard, "only Roles assigned to delegating User can be granted")).
                    addPropertyNode("roles").addConstraintViolation();

            isValid = false;
        }

        return isValid;
    }
}
