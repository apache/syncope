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

import static org.apache.syncope.core.persistence.jpa.validation.entity.AbstractValidator.LOG;

import javax.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;

public class SAML2IdPValidator extends AbstractValidator<SAML2IdPCheck, SAML2IdP> {

    @Override
    public boolean isValid(final SAML2IdP value, final ConstraintValidatorContext context) {
        long connObjectKeys = IterableUtils.countMatches(value.getMappingItems(), new Predicate<MappingItem>() {

            @Override
            public boolean evaluate(final MappingItem item) {
                return item.isConnObjectKey();
            }
        });
        if (!value.getMappingItems().isEmpty() && connObjectKeys != 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "Single ConnObjectKey mapping is required")).
                    addPropertyNode("connObjectKey.size").addConstraintViolation();
            return false;
        }

        boolean isValid = true;

        long passwords = IterableUtils.countMatches(value.getMappingItems(), new Predicate<MappingItem>() {

            @Override
            public boolean evaluate(final MappingItem item) {
                return item.isPassword();
            }
        });
        if (passwords > 0) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "No password mapping is allowed")).
                    addPropertyNode("password.size").addConstraintViolation();
            isValid = false;
        }

        for (MappingItem item : value.getMappingItems()) {
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

}
