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
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Role;

public class RoleValidator extends AbstractValidator<RoleCheck, Role> {

    @Override
    public boolean isValid(final Role role, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (role.getKey() == null
                || (!RoleDAO.GROUP_OWNER_ROLE.equals(role.getKey())
                && !Entity.ID_PATTERN.matcher(role.getKey()).matches())) {

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, role.getKey())).
                    addPropertyNode("key").addConstraintViolation();
            return false;
        }

        return true;
    }
}
