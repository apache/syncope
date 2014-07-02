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
package org.apache.syncope.core.sync.impl;

import org.apache.syncope.core.sync.SyncProfile;

import java.util.Iterator;

import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.sync.DefaultSyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A SyncActions implementation which allows the ability to import passwords from a Database
 * backend, where the passwords are hashed according to the password cipher algorithm property
 * of the (DB) Connector and HEX-encoded.
 */
public class DBPasswordSyncActions extends DefaultSyncActions {

    private static final Logger LOG = LoggerFactory.getLogger(DBPasswordSyncActions.class);

    private static final String CLEARTEXT = "CLEARTEXT";

    @Autowired
    private UserDAO userDAO;

    private String encodedPassword;

    private CipherAlgorithm cipher;

    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO> SyncDelta beforeProvision(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException {

        if (subject instanceof UserTO) {
            String password = ((UserTO) subject).getPassword();
            parseEncodedPassword(password, profile.getConnector());
        }

        return delta;
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final K subjectMod) throws JobExecutionException {

        if (subjectMod instanceof UserMod) {
            String modPassword = ((UserMod) subjectMod).getPassword();
            parseEncodedPassword(modPassword, profile.getConnector());
        }

        return delta;
    }

    private void parseEncodedPassword(final String password, final Connector connector) {
        if (password != null) {
            ConnInstance connInstance = connector.getActiveConnInstance();

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
        String cipherAlgorithm = CLEARTEXT;
        for (Iterator<ConnConfProperty> propertyIterator = connInstance.getConfiguration().iterator();
                propertyIterator.hasNext();) {

            ConnConfProperty property = propertyIterator.next();
            if ("cipherAlgorithm".equals(property.getSchema().getName())
                    && property.getValues() != null && !property.getValues().isEmpty()) {

                return (String) property.getValues().get(0);
            }
        }
        return cipherAlgorithm;
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO> void after(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final SyncResult result) throws JobExecutionException {

        if (subject instanceof UserTO && encodedPassword != null && cipher != null) {
            SyncopeUser syncopeUser = userDAO.find(subject.getId());
            if (syncopeUser != null) {
                syncopeUser.setEncodedPassword(encodedPassword.toUpperCase(), cipher);
            }
            encodedPassword = null;
            cipher = null;
        }
    }
}
