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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.identityconnectors.framework.common.objects.ObjectClass;

public class ExternalResourceValidator extends AbstractValidator<ExternalResourceCheck, ExternalResource> {

    private boolean isValid(final Mapping mapping, final ConstraintValidatorContext context) {
        if (mapping == null) {
            return true;
        }

        long connObjectKeys = IterableUtils.countMatches(mapping.getItems(), new Predicate<MappingItem>() {

            @Override
            public boolean evaluate(final MappingItem item) {
                return item.isConnObjectKey();
            }
        });
        if (connObjectKeys != 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "Single ConnObjectKey mapping is required")).
                    addPropertyNode("connObjectKey.size").addConstraintViolation();
            return false;
        }

        boolean isValid = true;

        long passwords = IterableUtils.countMatches(mapping.getItems(), new Predicate<MappingItem>() {

            @Override
            public boolean evaluate(final MappingItem item) {
                return item.isPassword();
            }
        });
        if (passwords > 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "One password mapping is allowed at most")).
                    addPropertyNode("password.size").addConstraintViolation();
            isValid = false;
        }

        for (MappingItem item : mapping.getItems()) {
            for (String className : item.getMappingItemTransformerClassNames()) {
                Class<?> actionsClass = null;
                boolean isAssignable = false;
                try {
                    actionsClass = Class.forName(className);
                    isAssignable = MappingItemTransformer.class.isAssignableFrom(actionsClass);
                } catch (Exception e) {
                    LOG.error("Invalid MappingItemTransformer specified: {}", className, e);
                }

                if (actionsClass == null || !isAssignable) {
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidMapping,
                                    "Invalid mapping item trasformer class name")).
                            addPropertyNode("mappingItemTransformerClassName").addConstraintViolation();
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    @Override
    public boolean isValid(final ExternalResource resource, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (resource.getKey() == null || !KEY_PATTERN.matcher(resource.getKey()).matches()) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, "Invalid resource key")).
                    addPropertyNode("key").addConstraintViolation();
            return false;
        }

        if (!resource.getPropagationActionsClassNames().isEmpty()) {
            for (String className : resource.getPropagationActionsClassNames()) {
                Class<?> actionsClass = null;
                boolean isAssignable = false;
                try {
                    actionsClass = Class.forName(className);
                    isAssignable = PropagationActions.class.isAssignableFrom(actionsClass);
                } catch (Exception e) {
                    LOG.error("Invalid PropagationActions specified: {}", className, e);
                }

                if (actionsClass == null || !isAssignable) {
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidResource, "Invalid actions class name")).
                            addPropertyNode("actionsClassName").addConstraintViolation();
                    return false;
                }
            }
        }

        final Set<AnyType> anyTypes = new HashSet<>();
        final Set<String> objectClasses = new HashSet<>();
        boolean validMappings = IterableUtils.matchesAll(resource.getProvisions(), new Predicate<Provision>() {

            @Override
            public boolean evaluate(final Provision provision) {
                anyTypes.add(provision.getAnyType());
                if (provision.getObjectClass() != null) {
                    objectClasses.add(provision.getObjectClass().getObjectClassValue());
                }
                return isValid(provision.getMapping(), context);
            }
        });

        if (anyTypes.size() < resource.getProvisions().size()) {
            context.buildConstraintViolationWithTemplate(getTemplate(EntityViolationType.InvalidResource,
                    "Each provision requires a different " + AnyType.class.getSimpleName())).
                    addPropertyNode("provisions").addConstraintViolation();
            return false;
        }
        if (objectClasses.size() < resource.getProvisions().size()) {
            context.buildConstraintViolationWithTemplate(getTemplate(EntityViolationType.InvalidResource,
                    "Each provision requires a different" + ObjectClass.class.getSimpleName())).
                    addPropertyNode("provisions").addConstraintViolation();
            return false;
        }

        return validMappings;
    }
}
