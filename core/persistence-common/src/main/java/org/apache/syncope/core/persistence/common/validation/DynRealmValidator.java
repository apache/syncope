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
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.DynRealm;

public class DynRealmValidator extends AbstractValidator<DynRealmCheck, DynRealm> {

    private static final Pattern REALM_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9]+");

    @Override
    public boolean isValid(final DynRealm dynRealm, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (dynRealm.getKey().startsWith("/") || !REALM_KEY_PATTERN.matcher(dynRealm.getKey()).matches()) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidDynRealm,
                            "Only letters and numbers are allowed in dynamic realm key, and must not start with /")).
                    addPropertyNode("key").addConstraintViolation();
        }

        return isValid;
    }
}
