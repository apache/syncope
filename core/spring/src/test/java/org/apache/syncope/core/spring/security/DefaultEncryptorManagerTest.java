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
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultEncryptorManagerTest {

    private static final String OTHER_KEY = "2bcdefghilmnopqr";

    private DefaultCredentialChecker credentialChecker;

    private DefaultEncryptorManager manager;

    @BeforeEach
    void setUp() {
        credentialChecker = mock(DefaultCredentialChecker.class);

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAesSecretKey(SpringTestConfiguration.AES_SECRET_KEY);
        securityProperties.setProductionMode(false);

        manager = new DefaultEncryptorManager(credentialChecker, securityProperties);
    }

    @Test
    void configuredKeyIsUsedAndCached() {
        Encryptor first = manager.getInstance();
        Encryptor second = manager.getInstance();

        assertSame(first, second);
        verify(credentialChecker, times(2)).checkIsDefaultAESKeyInUse();
    }

    @Test
    void explicitKeyIsUsedAndCachedSeparately() {
        Encryptor configured = manager.getInstance();
        Encryptor explicit = manager.getInstance(OTHER_KEY);
        Encryptor explicitAgain = manager.getInstance(OTHER_KEY);

        assertSame(explicit, explicitAgain);
        assertNotSame(configured, explicit);
    }

    @Test
    void blankKeyFallsBackToConfiguredKey() {
        Encryptor configured = manager.getInstance();
        Encryptor blank = manager.getInstance("   ");

        assertSame(configured, blank);
    }
}
