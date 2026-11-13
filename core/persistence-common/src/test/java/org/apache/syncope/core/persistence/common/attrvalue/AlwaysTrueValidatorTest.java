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

import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlwaysTrueValidatorTest {

    @Mock
    private PlainSchema schema;

    private final AlwaysTrueValidator validator = new AlwaysTrueValidator();

    @Test
    void doValidateTrueValue() {
        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBooleanValue(true);

        assertDoesNotThrow(() -> validator.doValidate(schema, attrValue));
    }

    @Test
    void doValidateFalseValue() {
        PlainAttrValue attrValue = new PlainAttrValue();
        attrValue.setBooleanValue(false);

        InvalidPlainAttrValueException ex = assertThrows(
                InvalidPlainAttrValueException.class,
                () -> validator.doValidate(schema, attrValue));

        assertEquals("This attribute must be set to \"true\"", ex.getMessage());
    }

}
