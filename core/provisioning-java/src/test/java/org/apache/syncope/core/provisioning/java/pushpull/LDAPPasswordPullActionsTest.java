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
package org.apache.syncope.core.provisioning.java.pushpull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

public class LDAPPasswordPullActionsTest extends AbstractTest {

    @Mock
    private SyncDelta syncDelta;

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
    private UserDAO userDAO;

    @Mock
    private ProvisioningReport result;

    @InjectMocks
    private LDAPPasswordPullActions ldapPasswordPullActions;

    private UserTO userTO;

    private UserPatch userPatch;

    private String encodedPassword;

    private CipherAlgorithm cipher;

    @BeforeEach
    public void initTest() {
        userTO = new UserTO();
        encodedPassword = "s3cureP4ssw0rd";
        cipher = CipherAlgorithm.SHA512;

        ReflectionTestUtils.setField(ldapPasswordPullActions, "encodedPassword", encodedPassword);
        ReflectionTestUtils.setField(ldapPasswordPullActions, "cipher", cipher);
    }

    @Test
    public void beforeProvision() throws JobExecutionException {
        String digest = "SHA256";
        String password = "t3stPassw0rd";
        userTO.setPassword(String.format("{%s}%s", digest, password));

        ldapPasswordPullActions.beforeProvision(profile, syncDelta, userTO);

        assertEquals(CipherAlgorithm.valueOf(digest), ReflectionTestUtils.getField(ldapPasswordPullActions, "cipher"));
        assertEquals(password, ReflectionTestUtils.getField(ldapPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void beforeUpdate() throws JobExecutionException {
        userPatch = new UserPatch();
        userPatch.setPassword(new PasswordPatch.Builder().value("{MD5}an0therTestP4ss").build());

        ldapPasswordPullActions.beforeUpdate(profile, syncDelta, userTO, userPatch);

        assertNull(ReflectionTestUtils.getField(ldapPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void afterWithNullUser() throws JobExecutionException {
        when(userDAO.find(userTO.getKey())).thenReturn(null);

        ldapPasswordPullActions.after(profile, syncDelta, userTO, result);

        assertNull(ReflectionTestUtils.getField(ldapPasswordPullActions, "encodedPassword"));
        assertNull(ReflectionTestUtils.getField(ldapPasswordPullActions, "cipher"));
    }

    @Test
    public void after(@Mock User user) throws JobExecutionException {
        when(userDAO.find(userTO.getKey())).thenReturn(user);

        ldapPasswordPullActions.after(profile, syncDelta, userTO, result);

        verify(user).setEncodedPassword(anyString(), any(CipherAlgorithm.class));
        assertNull(ReflectionTestUtils.getField(ldapPasswordPullActions, "encodedPassword"));
        assertNull(ReflectionTestUtils.getField(ldapPasswordPullActions, "cipher"));
    }
}
