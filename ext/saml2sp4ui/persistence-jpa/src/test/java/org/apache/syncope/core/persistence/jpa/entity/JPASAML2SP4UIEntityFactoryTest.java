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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.junit.jupiter.api.Test;

class JPASAML2SP4UIEntityFactoryTest {

    private final JPASAML2SP4UIEntityFactory factory = new JPASAML2SP4UIEntityFactory();

    @Test
    void supportedEntities() {
        SAML2SP4UIIdP idp = factory.newEntity(SAML2SP4UIIdP.class);
        assertInstanceOf(JPASAML2SP4UIIdP.class, idp);
        assertNotNull(idp.getKey());
        assertFalse(idp.getKey().isBlank());

        SAML2SP4UIUserTemplate userTemplate = factory.newEntity(SAML2SP4UIUserTemplate.class);
        assertInstanceOf(JPASAML2SP4UIUserTemplate.class, userTemplate);
        assertNotNull(userTemplate.getKey());
    }

    @Test
    void unsupportedEntity() {
        assertThrows(IllegalArgumentException.class, () -> factory.newEntity(Implementation.class));
    }
}
