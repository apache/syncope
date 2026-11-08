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
package org.apache.syncope.core.persistence.common.attrvalue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValueValidator;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPlainAttrValidationManagerTest {

    @Mock
    private PlainSchema schema;

    @Mock
    private PlainAttrValue attrValue;

    private final DefaultPlainAttrValidationManager manager = new DefaultPlainAttrValidationManager();

    @Test
    void validateWithoutValidatorUsesBasicValidator() {
        when(schema.getValidator()).thenReturn((Implementation) null);

        assertDoesNotThrow(() -> manager.validate(schema, "value", attrValue));
    }

    @Test
    void validateWithCustomValidator() {
        Implementation impl = mock(Implementation.class);
        when(schema.getValidator()).thenReturn(impl);

        PlainAttrValueValidator customValidator = mock(PlainAttrValueValidator.class);

        try (MockedStatic<ImplementationManager> im = mockStatic(ImplementationManager.class);
                MockedStatic<AuthContextUtils> ac = mockStatic(AuthContextUtils.class)) {
            ac.when(AuthContextUtils::getDomain).thenReturn("Master");
            im.when(() -> ImplementationManager.<PlainAttrValueValidator>build(
                    anyString(), any(Implementation.class),
                    any(), any()))
                .thenReturn(customValidator);

            manager.validate(schema, "value", attrValue);

            verify(customValidator).validate(schema, "value", attrValue);
        }
    }

    @Test
    void validateWithCustomValidatorBuildFailsFallsBackToBasic() {
        Implementation impl = mock(Implementation.class);
        when(schema.getValidator()).thenReturn(impl);

        try (MockedStatic<ImplementationManager> im = mockStatic(ImplementationManager.class);
                MockedStatic<AuthContextUtils> ac = mockStatic(AuthContextUtils.class)) {
            ac.when(AuthContextUtils::getDomain).thenReturn("Master");
            im.when(() -> ImplementationManager.<PlainAttrValueValidator>build(
                    anyString(), any(Implementation.class),
                    any(), any()))
                .thenThrow(new RuntimeException("build failed"));

            // BASIC_VALIDATOR is a no-op, so no exception is expected
            assertDoesNotThrow(() -> manager.validate(schema, "value", attrValue));
        }
    }

    @Test
    void validatePropagatesInvalidPlainAttrValueException() {
        when(schema.getValidator()).thenReturn((Implementation) null);

        // Use a real schema with a Binary type and an invalid value to trigger parsing/validation
        // But here we just verify the manager delegates to the validator's validate method.
        // The BasicValidator is a no-op, so we use a custom validator that throws.
        Implementation impl = mock(Implementation.class);
        when(schema.getValidator()).thenReturn(impl);

        PlainAttrValueValidator throwingValidator = mock(PlainAttrValueValidator.class);
        doThrow(new InvalidPlainAttrValueException("custom error")).
                when(throwingValidator).validate(schema, "value", attrValue);

        try (MockedStatic<ImplementationManager> im = mockStatic(ImplementationManager.class);
                MockedStatic<AuthContextUtils> ac = mockStatic(AuthContextUtils.class)) {
            ac.when(AuthContextUtils::getDomain).thenReturn("Master");
            im.when(() -> ImplementationManager.<PlainAttrValueValidator>build(
                    anyString(), any(Implementation.class),
                    any(), any()))
                .thenReturn(throwingValidator);

            InvalidPlainAttrValueException ex = assertThrows(
                    InvalidPlainAttrValueException.class,
                    () -> manager.validate(schema, "value", attrValue));

            assertEquals("custom error", ex.getMessage());
        }
    }
}
