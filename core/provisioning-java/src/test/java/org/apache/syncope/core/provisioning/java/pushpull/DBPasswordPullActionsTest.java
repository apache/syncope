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

<<<<<<< HEAD
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
=======
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

public class DBPasswordPullActionsTest extends AbstractTest {

    @Mock
    private SyncDelta syncDelta;

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
<<<<<<< HEAD
=======
    private AnyPatch anyPatch;

    @Mock
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions
    private UserDAO userDAO;

    @Mock
    private ProvisioningReport result;

    @Mock
    private Connector connector;

    @InjectMocks
    private DBPasswordPullActions dBPasswordPullActions;

    @Mock
    private ConnInstance connInstance;

    private Set<ConnConfProperty> connConfProperties;

<<<<<<< HEAD
    private UserTO userTO;

    private UserPatch userPatch;
=======
    private EntityTO entity;
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions

    private String encodedPassword;

    private CipherAlgorithm cipher;

    private ConnConfProperty connConfProperty;

    @BeforeEach
    public void initTest() {
<<<<<<< HEAD
        userTO = new UserTO();
=======
        entity = new UserTO();
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions
        encodedPassword = "s3cureP4ssw0rd";
        cipher = CipherAlgorithm.SHA512;
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
        connConfPropSchema.setName("cipherAlgorithm");
        connConfProperty = new ConnConfProperty();
        connConfProperty.setSchema(connConfPropSchema);
        connConfProperties = new HashSet<>();
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
<<<<<<< HEAD
        userTO.setPassword(password);
        connConfProperty.getValues().clear();
        connConfProperty.getValues().add(digest);

        dBPasswordPullActions.beforeProvision(profile, syncDelta, userTO);
=======
        ReflectionTestUtils.setField(entity, "password", password);
        ReflectionTestUtils.setField(connConfProperty, "values",
                new ArrayList<>(Collections.singletonList(digest)));

        dBPasswordPullActions.beforeProvision(profile, syncDelta, entity);
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions

        assertEquals(CipherAlgorithm.valueOf(digest), ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
        assertEquals(password, ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void beforeUpdate() throws JobExecutionException {
<<<<<<< HEAD
        userPatch = new UserPatch();
        userPatch.setPassword(new PasswordPatch.Builder().value("an0therTestP4ss").build());

        dBPasswordPullActions.beforeUpdate(profile, syncDelta, userTO, userPatch);
=======
        anyPatch = new UserPatch();
        PasswordPatch passwordPatch = new PasswordPatch();
        String password = "an0therTestP4ss";
        ReflectionTestUtils.setField(passwordPatch, "value", password);
        ReflectionTestUtils.setField(anyPatch, "password", passwordPatch);

        dBPasswordPullActions.beforeUpdate(profile, syncDelta, entity, anyPatch);
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions

        assertEquals(cipher, ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
        assertEquals(encodedPassword, ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
    }

    @Test
    public void after(@Mock User user) throws JobExecutionException {
<<<<<<< HEAD
        when(userDAO.find(user.getKey())).thenReturn(user);

        dBPasswordPullActions.after(profile, syncDelta, userTO, result);
=======
        when(userDAO.find(entity.getKey())).thenReturn(user);

        dBPasswordPullActions.after(profile, syncDelta, entity, result);
>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions

        verify(user).setEncodedPassword(anyString(), any(CipherAlgorithm.class));
        assertNull(ReflectionTestUtils.getField(dBPasswordPullActions, "encodedPassword"));
        assertNull(ReflectionTestUtils.getField(dBPasswordPullActions, "cipher"));
    }
<<<<<<< HEAD
=======

>>>>>>> 20fe766ff... Completed tests for DBPasswordPullActions
}
