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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.junit.jupiter.api.Test;

class DropdownFormTypeTest {

    private final DropdownFormType formType = new DropdownFormType("myProvider");

    @Test
    void information() {
        assertEquals("myProvider", formType.getInformation(DropdownValueProvider.NAME));
        assertNull(formType.getInformation("other"));
    }

    @Test
    void conversions() {
        assertEquals("value", formType.convertFormValueToModelValue("value"));
        assertEquals("42", formType.convertModelValueToFormValue(42));
        assertNull(formType.convertModelValueToFormValue(null));
    }
}
