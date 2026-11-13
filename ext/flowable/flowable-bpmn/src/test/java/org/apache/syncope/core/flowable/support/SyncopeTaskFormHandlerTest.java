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
package org.apache.syncope.core.flowable.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.form.AbstractFormType;
import org.junit.jupiter.api.Test;

class SyncopeTaskFormHandlerTest {

    private final ExpressionManager expressionManager = mock(ExpressionManager.class);

    @Test
    void dropdown() {
        FormProperty formProperty = new FormProperty();
        formProperty.setType("dropdown");
        FormValue provider = new FormValue();
        provider.setId(DropdownValueProvider.NAME);
        provider.setName("myProvider");
        formProperty.getFormValues().add(provider);

        Optional<AbstractFormType> formType =
                SyncopeTaskFormHandler.parseFormPropertyType(formProperty, expressionManager);

        DropdownFormType dropdown = assertInstanceOf(DropdownFormType.class, formType.orElseThrow());
        assertEquals("dropdown", dropdown.getName());
        assertEquals("myProvider", dropdown.getInformation(DropdownValueProvider.NAME));
    }

    @Test
    void dropdownWithoutValueProvider() {
        FormProperty formProperty = new FormProperty();
        formProperty.setType("dropdown");

        assertTrue(SyncopeTaskFormHandler.parseFormPropertyType(formProperty, expressionManager).isEmpty());

        FormValue wrongProvider = new FormValue();
        wrongProvider.setId("notTheExpectedId");
        formProperty.getFormValues().add(wrongProvider);

        assertTrue(SyncopeTaskFormHandler.parseFormPropertyType(formProperty, expressionManager).isEmpty());
    }

    @Test
    void password() {
        FormProperty formProperty = new FormProperty();
        formProperty.setType("password");

        AbstractFormType formType = SyncopeTaskFormHandler.parseFormPropertyType(formProperty, expressionManager).
                orElseThrow();

        assertInstanceOf(PasswordFormType.class, formType);
        assertEquals("password", formType.getName());
    }

    @Test
    void notCustom() {
        FormProperty formProperty = new FormProperty();
        formProperty.setType("string");

        assertTrue(SyncopeTaskFormHandler.parseFormPropertyType(formProperty, expressionManager).isEmpty());
    }
}
