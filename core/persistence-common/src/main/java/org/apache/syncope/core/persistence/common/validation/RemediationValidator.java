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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Remediation;

public class RemediationValidator extends AbstractValidator<RemediationCheck, Remediation> {

    @Override
    public boolean isValid(final Remediation remediation, final ConstraintValidatorContext context) {
        boolean isValid = true;

        switch (remediation.getOperation()) {
            case CREATE:
                if (remediation.getPayloadAsCR(remediation.getAnyType().getKind().getCRClass()) == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidRemediation,
                                    "Expected " + remediation.getAnyType().getKind().getTOClass().getName())).
                            addPropertyNode("payload").addConstraintViolation();

                    isValid = false;
                }
                break;

            case UPDATE:
                if (remediation.getPayloadAsUR(remediation.getAnyType().getKind().getURClass()) == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidRemediation,
                                    "Expected " + remediation.getAnyType().getKind().getURClass().getName())).
                            addPropertyNode("payload").addConstraintViolation();

                    isValid = false;
                }
                break;

            case DELETE:
                if (!SyncopeConstants.UUID_PATTERN.matcher(remediation.getPayloadAsKey()).matches()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidRemediation, "Expected UUID")).
                            addPropertyNode("payload").addConstraintViolation();

                    isValid = false;
                }
                break;

            case NONE:
            default:
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidRemediation, "NONE is not allowed")).
                        addPropertyNode("operation").addConstraintViolation();

                isValid = false;
        }

        return isValid;
    }
}
