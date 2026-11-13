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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SAML2SP4UIPropertiesTest {

    @Test
    void defaults() {
        SAML2SP4UIProperties props = new SAML2SP4UIProperties();

        assertEquals(3600, props.getMaximumAuthenticationLifetime());
        assertEquals(300, props.getAcceptedSkew());
    }

    @Test
    void setters() {
        SAML2SP4UIProperties props = new SAML2SP4UIProperties();
        props.setKeystore("keystore");
        props.setKeystoreType("JKS");
        props.setKeystoreStorepass("storepass");
        props.setKeystoreKeypass("keypass");
        props.setKeystoreAlias("alias");
        props.setMaximumAuthenticationLifetime(60);
        props.setAcceptedSkew(10);

        assertEquals("keystore", props.getKeystore());
        assertEquals("JKS", props.getKeystoreType());
        assertEquals("storepass", props.getKeystoreStorepass());
        assertEquals("keypass", props.getKeystoreKeypass());
        assertEquals("alias", props.getKeystoreAlias());
        assertEquals(60, props.getMaximumAuthenticationLifetime());
        assertEquals(10, props.getAcceptedSkew());
    }
}
