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
import java.util.Optional;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;

public class AnyValidator extends AbstractValidator<AnyCheck, Any> {

    @Override
    public boolean isValid(final Any any, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (any.getUManager() != null && any.getGManager() != null) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidManager,
                            "Must either be managed by an user or by a group, not both")).
                    addPropertyNode("manager").addConstraintViolation();
            return false;
        }

        AllowedSchemas<PlainSchema> allowedPlainSchemas =
                ApplicationContextProvider.getApplicationContext().getBean(AnyUtilsFactory.class).
                        getInstance(any.getType().getKind()).dao().findAllowedSchemas(any, PlainSchema.class);

        for (PlainAttr attr : any.getPlainAttrs()) {
            String plainSchema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
            if (plainSchema != null && !allowedPlainSchemas.selfContains(plainSchema)) {
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidPlainAttr,
                                plainSchema + " not allowed for this instance")).
                        addPropertyNode("plainAttrs").addConstraintViolation();
                return false;
            }
        }
        if (any instanceof Groupable<?, ?, ?> groupable) {
            for (Membership<?> membership : groupable.getMemberships()) {
                for (PlainAttr attr : groupable.getPlainAttrs(membership)) {
                    String plainSchema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
                    if (plainSchema != null
                            && !allowedPlainSchemas.membershipsContains(membership.getRightEnd(), plainSchema)) {

                        context.buildConstraintViolationWithTemplate(
                                getTemplate(EntityViolationType.InvalidPlainAttr,
                                        plainSchema + " not allowed for membership of group "
                                        + membership.getRightEnd().getName())).
                                addPropertyNode("plainAttrs").addConstraintViolation();
                        return false;
                    }
                }
            }
        }
        if (any instanceof Relatable<?, ?> relatable) {
            for (Relationship<?, ?> relationship : relatable.getRelationships()) {
                for (PlainAttr attr : relatable.getPlainAttrs(relationship)) {
                    String plainSchema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
                    if (plainSchema != null
                            && !allowedPlainSchemas.relationshipTypesContains(relationship.getType(), plainSchema)) {

                        context.buildConstraintViolationWithTemplate(
                                getTemplate(EntityViolationType.InvalidPlainAttr,
                                        plainSchema + " not allowed for relationships of type "
                                        + relationship.getType().getKey())).
                                addPropertyNode("plainAttrs").addConstraintViolation();
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
