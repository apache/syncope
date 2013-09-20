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
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.propagation.PropagationActions;

public class ExternalResourceValidator extends AbstractValidator<ExternalResourceCheck, ExternalResource> {

    private boolean isValid(final AbstractMappingItem item, final ConstraintValidatorContext context) {
        if (StringUtils.isBlank(item.getExtAttrName())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, item + ".extAttrName is null")).
                    addNode("extAttrName").addConstraintViolation();

            return false;
        }

        if (StringUtils.isBlank(item.getIntAttrName())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, item + ".intAttrName is null")).
                    addNode("intAttrName").addConstraintViolation();

            return false;
        }

        if (item.getPurpose() == null) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, item + ".purpose is null")).
                    addNode("purpose").addConstraintViolation();

            return false;
        }

        return true;
    }

    private boolean isValid(final AbstractMapping mapping, final ConstraintValidatorContext context) {
        if (mapping == null) {
            return true;
        }

        int accountIds = 0;
        for (AbstractMappingItem item : mapping.getItems()) {
            if (item.isAccountid()) {
                accountIds++;
            }
        }
        if (accountIds != 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "One and only one accountId mapping is needed")).
                    addNode("accountId.size").addConstraintViolation();
            return false;
        }

        boolean isValid = true;

        int passwords = 0;
        for (AbstractMappingItem item : mapping.getItems()) {
            isValid &= isValid(item, context);

            if (item.isPassword()) {
                passwords++;
            }
        }
        if (passwords > 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "One and only one password mapping is allowed")).
                    addNode("password.size").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    @Override
    public boolean isValid(final ExternalResource resource, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!NAME_PATTERN.matcher(resource.getName()).matches()) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, "Invalid Resource name")).
                    addNode("name").addConstraintViolation();
            return false;
        }

        if (StringUtils.isNotBlank(resource.getPropagationActionsClassName())) {
            Class<?> actionsClass = null;
            boolean isAssignable = false;
            try {
                actionsClass = Class.forName(resource.getPropagationActionsClassName());
                isAssignable = PropagationActions.class.isAssignableFrom(actionsClass);
            } catch (Exception e) {
                LOG.error("Invalid PropagationActions specified: {}", resource.getPropagationActionsClassName(), e);
            }

            if (actionsClass == null || !isAssignable) {
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidResource, "Ivalid actions class name")).
                        addNode("actionsClassName").addConstraintViolation();
                return false;
            }
        }

        return isValid(resource.getUmapping(), context) && isValid(resource.getRmapping(), context);
    }
}
