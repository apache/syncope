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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2SP4UIIdPValidatorTest {

    private ConstraintValidatorContext context;

    private SAML2SP4UIIdPValidator validator;

    @BeforeEach
    void setUp() {
        context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        validator = new SAML2SP4UIIdPValidator();
    }

    private static Item item(final boolean connObjectKey, final boolean password) {
        Item item = new Item();
        item.setConnObjectKey(connObjectKey);
        item.setPassword(password);
        return item;
    }

    private static SAML2SP4UIIdP idp(final String key, final List<Item> items) {
        SAML2SP4UIIdP idp = mock(SAML2SP4UIIdP.class);
        when(idp.getKey()).thenReturn(key);
        when(idp.getItems()).thenReturn(items);
        return idp;
    }

    @Test
    void htmlInKey() {
        assertFalse(validator.isValid(idp("<script>", List.of()), context));

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void selfRegAndCreateUnmatchingCannotBeBothSet() {
        SAML2SP4UIIdP idp = idp("idp", List.of());
        when(idp.isSelfRegUnmatching()).thenReturn(true);
        when(idp.isCreateUnmatching()).thenReturn(true);

        assertFalse(validator.isValid(idp, context));
    }

    @Test
    void singleConnObjectKeyIsRequiredWhenItemsAreMapped() {
        assertFalse(validator.isValid(idp("idp", List.of(item(false, false))), context));
        assertFalse(validator.isValid(idp("idp", List.of(item(true, false), item(true, false))), context));
    }

    @Test
    void passwordMappingIsNotAllowed() {
        assertFalse(validator.isValid(idp("idp", List.of(item(true, true))), context));
    }

    @Test
    void validMappings() {
        assertTrue(validator.isValid(idp("idp", List.of()), context));
        assertTrue(validator.isValid(idp("idp", List.of(item(true, false), item(false, false))), context));
    }
}
