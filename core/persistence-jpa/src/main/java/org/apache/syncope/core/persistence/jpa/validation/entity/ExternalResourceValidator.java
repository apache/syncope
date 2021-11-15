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
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.identityconnectors.framework.common.objects.ObjectClass;

public class ExternalResourceValidator extends AbstractValidator<ExternalResourceCheck, ExternalResource> {

    private static boolean isValid(final List<? extends Item> items, final ConstraintValidatorContext context) {
        long connObjectKeys = items.stream().filter(Item::isConnObjectKey).count();
        if (connObjectKeys != 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "Single ConnObjectKey mapping is required")).
                    addPropertyNode("connObjectKey.size").addConstraintViolation();
            return false;
        }

        return true;
    }

    private static boolean isValid(final OrgUnit orgUnit, final ConstraintValidatorContext context) {
        if (orgUnit == null) {
            return true;
        }

        return isValid(orgUnit.getItems(), context);
    }

    private static boolean isValid(final Mapping mapping, final ConstraintValidatorContext context) {
        if (mapping == null) {
            return true;
        }

        boolean isValid = true;

        long passwords = mapping.getItems().stream().filter(MappingItem::isPassword).count();
        if (passwords > 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "One password mapping is allowed at most")).
                    addPropertyNode("password.size").addConstraintViolation();
            isValid = false;
        }

        return isValid && isValid(mapping.getItems(), context);
    }

    @Override
    public boolean isValid(final ExternalResource resource, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (resource.getKey() == null || !Entity.ID_PATTERN.matcher(resource.getKey()).matches()) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, resource.getKey())).
                    addPropertyNode("key").addConstraintViolation();
            return false;
        }

        final Set<AnyType> anyTypes = new HashSet<>();
        final Set<String> objectClasses = new HashSet<>();
        boolean validMappings = resource.getProvisions().stream().allMatch(provision -> {
            anyTypes.add(provision.getAnyType());
            if (provision.getObjectClass() != null) {
                objectClasses.add(provision.getObjectClass().getObjectClassValue());
            }
            return isValid(provision.getMapping(), context);
        });
        validMappings &= isValid(resource.getOrgUnit(), context);

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
