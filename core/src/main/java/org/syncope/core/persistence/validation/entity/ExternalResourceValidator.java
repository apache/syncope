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
package org.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.types.EntityViolationType;

public class ExternalResourceValidator extends AbstractValidator
        implements ConstraintValidator<ExternalResourceCheck, ExternalResource> {

    @Override
    public void initialize(final ExternalResourceCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final ExternalResource object,
            final ConstraintValidatorContext context) {

        boolean isValid;

        if (object == null) {
            isValid = true;
        } else {
            int accountIds = 0;
            for (SchemaMapping mapping : object.getMappings()) {
                if (mapping.isAccountid()) {
                    accountIds++;
                }
            }
            isValid = accountIds == 1;

            if (!isValid) {
                LOG.error("Mappings for " + object
                        + " have 0 or >1 account ids");

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.InvalidAccountIdCount.toString()).
                        addNode(object + ".accountIds.size==" + accountIds).
                        addConstraintViolation();
            }
        }

        return isValid;
    }
}
