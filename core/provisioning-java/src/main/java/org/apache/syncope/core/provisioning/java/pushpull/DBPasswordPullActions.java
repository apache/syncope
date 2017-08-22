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

import java.util.Optional;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A {@link org.apache.syncope.core.provisioning.api.pushpull.PullActions} implementation which allows the ability to
 * import passwords from a Database backend, where the passwords are hashed according to the password cipher algorithm
 * property of the (DB) Connector and HEX-encoded.
 */
public class DBPasswordPullActions implements PullActions {

    private static final Logger LOG = LoggerFactory.getLogger(DBPasswordPullActions.class);

    private static final String CLEARTEXT = "CLEARTEXT";

    @Autowired
    private UserDAO userDAO;

    private String encodedPassword;

    private CipherAlgorithm cipher;

    @Transactional(readOnly = true)
    @Override
    public SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO any) throws JobExecutionException {

        if (any instanceof UserTO) {
            String password = ((UserTO) any).getPassword();
            parseEncodedPassword(password, profile.getConnector());
        }

        return delta;
    }

    @Transactional(readOnly = true)
    @Override
    public <M extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entityTO,
            final M anyPatch) throws JobExecutionException {

        if (anyPatch instanceof UserPatch) {
            PasswordPatch modPassword = ((UserPatch) anyPatch).getPassword();
            parseEncodedPassword(modPassword == null ? null : modPassword.getValue(), profile.getConnector());
        }

        return delta;
    }

    private void parseEncodedPassword(final String password, final Connector connector) {
        if (password != null) {
            ConnInstance connInstance = connector.getConnInstance();

            String cipherAlgorithm = getCipherAlgorithm(connInstance);
            if (!CLEARTEXT.equals(cipherAlgorithm)) {
                try {
                    encodedPassword = password;
                    cipher = CipherAlgorithm.valueOf(cipherAlgorithm);
                } catch (IllegalArgumentException e) {
                    LOG.error("Cipher algorithm not allowed: {}", cipherAlgorithm, e);
                    encodedPassword = null;
                }
            }
        }
    }

    private String getCipherAlgorithm(final ConnInstance connInstance) {
        Optional<ConnConfProperty> cipherAlgorithm = connInstance.getConf().stream().
                filter(property -> "cipherAlgorithm".equals(property.getSchema().getName())
                && property.getValues() != null && !property.getValues().isEmpty()).findFirst();

        return cipherAlgorithm.isPresent()
                ? (String) cipherAlgorithm.get().getValues().get(0)
                : CLEARTEXT;
    }

    @Transactional
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO any,
            final ProvisioningReport result) throws JobExecutionException {

        if (any instanceof UserTO && encodedPassword != null && cipher != null) {
            User user = userDAO.find(any.getKey());
            if (user != null) {
                user.setEncodedPassword(encodedPassword.toUpperCase(), cipher);
            }
            encodedPassword = null;
            cipher = null;
        }
    }
}
