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

import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.sync.DefaultSyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.transaction.annotation.Transactional;

/**
 * A SyncActions implementation which allows the ability to import passwords from an LDAP backend
 * that are hashed.
 */
public class LDAPPasswordSyncActions extends DefaultSyncActions {
    
    protected static final Logger LOG = LoggerFactory.getLogger(LDAPPasswordSyncActions.class);

    @Autowired
    protected UserDAO userDAO;

    private String encodedPassword;
    private CipherAlgorithm cipher;

    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO> SyncDelta beforeCreate(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final SyncDelta delta,
            final T subject) throws JobExecutionException {
        if (subject instanceof UserTO) {
            String password = ((UserTO)subject).getPassword();
            if (password != null && password.startsWith("{")) {
                int closingBracketIndex = password.indexOf('}');
                String digest = password.substring(1, password.indexOf('}'));
                CipherAlgorithm cipherAlgorithm = CipherAlgorithm.fromString(digest);
                if (cipherAlgorithm != null) {
                    encodedPassword = password.substring(closingBracketIndex + 1);
                    cipher = cipherAlgorithm;
                }
            }
        }
        
        return delta;
    }
    
    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO> void after(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final SyncDelta delta,
            final T subject,
            final SyncResult result) throws JobExecutionException {

        if (subject instanceof UserTO && encodedPassword != null && cipher != null) {
            SyncopeUser syncopeUser = userDAO.find(subject.getId());
            if (syncopeUser != null) {
                byte[] encodedPasswordBytes = Base64.decode(encodedPassword.getBytes());
                char[] encodedHex = Hex.encode(encodedPasswordBytes);
                String encodedHexStr = new String(encodedHex).toUpperCase();
                
                /*UserMod userMod = new UserMod();
                userMod.setId(subject.getId());
                userMod.setPassword(encodedHexStr);
                uwfAdapter.update(userMod);*/
                syncopeUser.setEncodedPassword(encodedHexStr, cipher, 0);
            }
            encodedPassword = null;
            cipher = null;
        }
    }
    
}
