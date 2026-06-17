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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.throttle.PasswordResetThrottleException;
import org.apache.syncope.core.spring.security.throttle.ThrottlerAttempts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserSelfLogicPasswordResetTest {

    private static final Cache<String, ThrottlerAttempts> CACHE =
            Caching.getCachingProvider().getCacheManager().createCache(
                    UserSelfLogicPasswordResetTest.class.getName(),
                    new MutableConfiguration<>());

    private UserDAO userDAO;

    private SecurityProperties securityProperties;

    private UserSelfLogic logic;

    @BeforeEach
    void setUp() {
        ConfParamOps confParamOps = mock(ConfParamOps.class);
        userDAO = mock(UserDAO.class);
        securityProperties = new SecurityProperties();

        when(confParamOps.get(any(), eq(StandardConfParams.PASSWORD_RESET_ALLOWED), eq(false), eq(boolean.class))).
                thenReturn(true);

        logic = new UserSelfLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                userDAO,
                mock(UserDataBinder.class),
                mock(UserProvisioningManager.class),
                mock(EncryptorManager.class),
                confParamOps,
                mock(DelegationDAO.class),
                mock(AccessTokenDAO.class),
                mock(ExternalResourceDAO.class),
                mock(RuleProvider.class),
                securityProperties,
                CACHE);
    }

    @Test
    void passwordResetRequestsAreThrottled() {
        securityProperties.getPasswordResetThrottle().setMaxAttempts(1);
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.requestPasswordReset("missing", "answer", "192.0.2.1"));
        assertThrows(
                PasswordResetThrottleException.class,
                () -> logic.requestPasswordReset("missing", "answer", "192.0.2.1"));
        assertThrows(NotFoundException.class, () -> logic.requestPasswordReset("missing", "answer", "192.0.2.2"));
    }

    @Test
    void passwordResetThrottlingCanBeDisabled() {
        securityProperties.getPasswordResetThrottle().setEnabled(false);
        securityProperties.getPasswordResetThrottle().setMaxAttempts(1);
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.requestPasswordReset("missing", "answer", "192.0.2.1"));
        assertThrows(NotFoundException.class, () -> logic.requestPasswordReset("missing", "answer", "192.0.2.1"));
    }
}
