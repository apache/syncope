/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.entity.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.junit.jupiter.api.Test;

class PropagationDataTest {

    @Test
    void attributesAreSetFromConstructor() {
        final Set<Attribute> attributes = new HashSet<>();
        final PropagationData propagationData = new PropagationData(attributes);

        assertEquals(attributes, propagationData.getAttributes());
    }

    @Test
    void attributeDeltasAreInitiallyNull() {
        final PropagationData propagationData = new PropagationData(new HashSet<>());

        assertNull(propagationData.getAttributeDeltas());
    }

    @Test
    void attributeDeltasAreSettable() {
        final PropagationData propagationData = new PropagationData(new HashSet<>());
        final Set<AttributeDelta> attributeDeltas = new HashSet<>();

        propagationData.setAttributeDeltas(attributeDeltas);

        assertEquals(attributeDeltas, propagationData.getAttributeDeltas());
    }

    @Test
    void toStringContainsAttributesAndDeltas() {
        final Set<Attribute> attributes = new HashSet<>();
        final Set<AttributeDelta> attributeDeltas = new HashSet<>();
        final PropagationData propagationData = new PropagationData(attributes);
        propagationData.setAttributeDeltas(attributeDeltas);

        final String toString = propagationData.toString();

        assertTrue(toString.contains("attributes"));
        assertTrue(toString.contains("attributeDeltas"));
        assertNotNull(toString);
    }
}
