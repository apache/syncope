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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class DBPasswordPullActionsTest extends AbstractTest {

    @Mock
    private SyncDelta syncDelta;

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
    private UserDAO userDAO;

    @Mock
    private ProvisioningReport result;

    @Mock
    private Connector connector;

    @InjectMocks
    private DBPasswordPullActions dBPasswordPullActions;

    @Mock
    private ConnInstance connInstance;

    private List<ConnConfProperty> connConfProperties;

    private UserTO userTO;

    private UserCR userCR;

    private UserUR userUR;

    private String encodedPassword;

    private CipherAlgorithm cipher;

    private ConnConfProperty connConfProperty;

    @BeforeEach
    public void initTest() {
        userTO = new UserTO();
        encodedPassword = "s3cureP4ssw0rd";
        cipher = CipherAlgorithm.SHA512;
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
        connConfPropSchema.setName("cipherAlgorithm");
        connConfProperty = new ConnConfProperty();
        connConfProperty.setSchema(connConfPropSchema);
        connConfProperties = new ArrayList<>();
        connConfProperties.add(connConfProperty);

        ReflectionTestUtils.setField(dBPasswordPullActions, "encodedPassword", encodedPassword);
        ReflectionTestUtils.setField(dBPasswordPullActions, "cipher", cipher);

        lenient().when(profile.getConnector()).thenReturn(connector);
        lenient().when(connector.getConnInstance()).thenReturn(connInstance);
        lenient().when(connInstance.getConf()).thenReturn(connConfProperties);
    }

    @Test
    public void beforeProvision() throws JobExecutionException {
        String digest = "SHA256";
        String password = "t3stPassw0rd";
        userCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").password(password).build();
        connConfProperty.getValues().clear();
        connConfProperty.getValues().add(digest);

        dBPasswordPullActions.beforeProvision(profile, syncDelta, userCR);
        userTO.setPassword(password);
        connConfProperty.getValues().clear();
        connConfProperty.getValues().add(digest);

        dBPasswordPullActions.beforeProvision(profile, syncDelta, userCR);

        assertEquals(CipherAlgorithm.valueOf(digest), ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
        assertEquals(password, ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void beforeUpdate() throws JobExecutionException {
        userUR = new UserUR.Builder(null).
                password(new PasswordPatch.Builder().value("an0therTestP4ss").build()).
                build();

        dBPasswordPullActions.beforeUpdate(profile, syncDelta, userTO, userUR);
        userUR = new UserUR();
        userUR.setPassword(new PasswordPatch.Builder().value("an0therTestP4ss").build());

        dBPasswordPullActions.beforeUpdate(profile, syncDelta, userTO, userUR);
        assertEquals(cipher, ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
        assertEquals(encodedPassword, ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void after(final @Mock User user) throws JobExecutionException {
        when(userDAO.findById(user.getKey())).thenAnswer(ic -> Optional.of(user));

        dBPasswordPullActions.after(profile, syncDelta, userTO, result);

        verify(user).setEncodedPassword(anyString(), any(CipherAlgorithm.class));
        assertNull(ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
        assertNull(ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
    }
}
