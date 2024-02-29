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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LDAPPasswordPullActionsTest extends AbstractTest {

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
    private UserDAO userDAO;

    @Mock
    private ProvisioningReport result;

    @InjectMocks

    private LDAPPasswordPullActions actions;

    @Test
    public void afterWithNoUser() throws JobExecutionException {
        UserTO userTO = new UserTO();
        userTO.setKey(UUID.randomUUID().toString());
        when(userDAO.findById(userTO.getKey())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> actions.after(profile, null, userTO, result));
    }

    @Test
    public void after(final @Mock User user) throws JobExecutionException {
        UserTO userTO = new UserTO();
        userTO.setKey(UUID.randomUUID().toString());
        when(userDAO.findById(userTO.getKey())).thenAnswer(ic -> Optional.of(user));

        Set<Attribute> attributes = new HashSet<>();
        attributes.add(new Uid(UUID.randomUUID().toString()));
        attributes.add(new Name(UUID.randomUUID().toString()));
        attributes.add(AttributeBuilder.buildPassword(
                new GuardedString("{SSHA}4AwQq1UVDwubSXmR4pnmLsoVR6U2Z7R55kwxRA==".toCharArray())));
        SyncDelta delta = new SyncDeltaBuilder().
                setToken(new SyncToken("sample-token")).
                setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                setUid(new Uid(UUID.randomUUID().toString())).
                setObject(new ConnectorObject(ObjectClass.ACCOUNT, attributes)).
                build();

        actions.after(profile, delta, userTO, result);

        verify(user).setEncodedPassword(anyString(), any(CipherAlgorithm.class));
    }
}
