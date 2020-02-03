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
package org.apache.syncope.core.provisioning.api.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class POJOHelperTest extends AbstractTest {

    @Test
    public void serialize() {
        Object object = 9001;

        assertEquals(String.valueOf(object), POJOHelper.serialize(object));
    }

    @Test
    public void serializeWithDefaultPrettyPrinter() {
        Object object = 9001;

        assertEquals(String.valueOf(object), POJOHelper.serializeWithDefaultPrettyPrinter(object));
    }

    @Test
    public void deserializeWithClassReference() {
        String serialized = "false";

        assertEquals(Boolean.valueOf(serialized), POJOHelper.deserialize(serialized, Object.class));
    }

    @Test
    public void deserializeWithTypeReference(@Mock TypeReference<? extends Object> reference) {
        String serialized = "false";

        assertNull(POJOHelper.deserialize(serialized, reference));
    }
}
