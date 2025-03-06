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
package org.apache.syncope.core.persistence.api.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.common.validation.AbstractValidator;

public class SAML2SP4UIIdPValidator extends AbstractValidator<SAML2SP4UIIdPCheck, SAML2SP4UIIdP> {

    @Override
    public boolean isValid(final SAML2SP4UIIdP saml2IdP, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (isHtml(saml2IdP.getKey())) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, saml2IdP.getKey())).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        }

        if (saml2IdP.isSelfRegUnmatching() && saml2IdP.isCreateUnmatching()) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.Standard,
                            "Either selfRegUnmatching or createUnmatching, not both")).
                    addPropertyNode("selfRegUnmatching").
                    addPropertyNode("createUnmatching").addConstraintViolation();

            return false;
        }

        long connObjectKeys = saml2IdP.getItems().stream().filter(Item::isConnObjectKey).count();
        if (!saml2IdP.getItems().isEmpty() && connObjectKeys != 1) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "Single ConnObjectKey mapping is required")).
                    addPropertyNode("connObjectKey.size").addConstraintViolation();

            return false;
        }

        boolean[] isValid = { true };

        long passwords = saml2IdP.getItems().stream().filter(Item::isPassword).count();
        if (passwords > 0) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidMapping, "No password mapping is allowed")).
                    addPropertyNode("password.size").addConstraintViolation();
            isValid[0] = false;
        }

        return isValid[0];
    }
}
