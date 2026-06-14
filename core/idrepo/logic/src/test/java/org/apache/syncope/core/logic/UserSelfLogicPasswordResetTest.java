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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserSelfLogicPasswordResetTest {

    private ConfParamOps confParamOps;

    private UserDAO userDAO;

    private UserProvisioningManager provisioningManager;

    private SecurityProperties securityProperties;

    private UserSelfLogic logic;

    @BeforeEach
    public void setUp() {
        confParamOps = mock(ConfParamOps.class);
        userDAO = mock(UserDAO.class);
        provisioningManager = mock(UserProvisioningManager.class);
        securityProperties = new SecurityProperties();

        when(confParamOps.get(any(), eq(StandardConfParams.PASSWORD_RESET_ALLOWED), eq(false), eq(boolean.class))).
                thenReturn(true);
        when(confParamOps.get(
                any(), eq(StandardConfParams.PASSWORD_RESET_SECURITY_QUESTION), eq(false), eq(boolean.class))).
                thenReturn(true);

        logic = new UserSelfLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                userDAO,
                mock(UserDataBinder.class),
                provisioningManager,
                mock(EncryptorManager.class),
                confParamOps,
                mock(DelegationDAO.class),
                mock(AccessTokenDAO.class),
                mock(ExternalResourceDAO.class),
                mock(RuleProvider.class),
                securityProperties);
    }

    @Test
    public void defaultPasswordResetHidesUnknownUser() {
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> logic.requestPasswordReset("missing", "answer"));
        verify(provisioningManager, never()).requestPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    public void passwordResetDetailsCanBeExposedForCompatibility() {
        securityProperties.getPasswordReset().setHideDetails(false);
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(
                NotFoundException.class,
                () -> logic.requestPasswordReset("missing", "answer"));
        assertTrue(e.getMessage().contains("missing"));
    }

    @Test
    public void defaultPasswordResetHidesInvalidSecurityAnswer() {
        when(userDAO.findKey("rossini")).thenReturn(Optional.of("user-key"));
        when(provisioningManager.checkSecurityAnswer("user-key", "wrong")).thenReturn(false);

        assertDoesNotThrow(() -> logic.requestPasswordReset("rossini", "wrong"));
        verify(provisioningManager, never()).requestPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    public void invalidSecurityAnswerDetailsCanBeExposedForCompatibility() {
        securityProperties.getPasswordReset().setHideDetails(false);
        when(userDAO.findKey("rossini")).thenReturn(Optional.of("user-key"));
        when(provisioningManager.checkSecurityAnswer("user-key", "wrong")).thenReturn(false);

        SyncopeClientException e = assertThrows(
                SyncopeClientException.class,
                () -> logic.requestPasswordReset("rossini", "wrong"));
        assertEquals(ClientExceptionType.InvalidSecurityAnswer, e.getType());
    }

    @Test
    public void defaultPasswordResetDoesNotReflectInvalidToken() {
        when(userDAO.findByToken("WRONG TOKEN")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(
                NotFoundException.class,
                () -> logic.confirmPasswordReset("WRONG TOKEN", "password"));
        assertFalse(e.getMessage().contains("WRONG TOKEN"));
    }

    @Test
    public void invalidTokenDetailsCanBeExposedForCompatibility() {
        securityProperties.getPasswordReset().setHideDetails(false);
        when(userDAO.findByToken("WRONG TOKEN")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(
                NotFoundException.class,
                () -> logic.confirmPasswordReset("WRONG TOKEN", "password"));
        assertTrue(e.getMessage().contains("WRONG TOKEN"));
    }
}
