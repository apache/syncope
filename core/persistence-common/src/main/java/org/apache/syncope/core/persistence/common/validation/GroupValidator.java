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
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public class GroupValidator extends AbstractValidator<GroupCheck, Group> {

    @Override
    public boolean isValid(final Group group, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (group.getUserOwner() != null && group.getGroupOwner() != null) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidGroupOwner,
                            "A group must either be owned by an user or a group, not both")).
                    addPropertyNode("owner").addConstraintViolation();
        }

        if (isValid && (group.getName() == null || !Entity.ID_PATTERN.matcher(group.getName()).matches())) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, group.getName())).
                    addPropertyNode("name").addConstraintViolation();
        }

        if (isValid) {
            Set<AnyType> anyTypes = new HashSet<>();
            for (ADynGroupMembership memb : group.getADynMemberships()) {
                anyTypes.add(memb.getAnyType());

                if (memb.getAnyType().getKind() != AnyTypeKind.ANY_OBJECT) {
                    isValid = false;

                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidADynMemberships,
                                    "No user or group dynamic membership condition are allowed here")).
                            addPropertyNode("aDynMemberships").addConstraintViolation();
                }
            }

            if (isValid && anyTypes.size() < group.getADynMemberships().size()) {
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidADynMemberships,
                                "Each dynamic membership condition requires a different "
                                + AnyType.class.getSimpleName())).
                        addPropertyNode("aDynMemberships").addConstraintViolation();
                return false;
            }

        }

        return isValid;
    }
}
