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
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;

public class RelationshipValidator extends AbstractValidator<RelationshipCheck, Relationship<?, ?>> {

    @Override
    public boolean isValid(final Relationship<?, ?> relationship, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        RelationshipType relationshipType = relationship.getType();

        if (!relationshipType.getLeftEndAnyType().equals(relationship.getLeftEnd().getType())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidAnyType, relationship.getLeftEnd().getType().getKey())).
                    addPropertyNode("leftEnd").addConstraintViolation();

            isValid = false;
        }

        if (!relationshipType.getRightEndAnyType().equals(relationship.getRightEnd().getType())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidAnyType, relationship.getRightEnd().getType().getKey())).
                    addPropertyNode("rightEnd").addConstraintViolation();

            isValid = false;
        }

        return isValid;
    }
}
