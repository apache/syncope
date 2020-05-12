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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPKeystore;
import org.apache.syncope.core.provisioning.api.data.SAML2SPKeystoreBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2SPKeystoreBinderImpl implements SAML2SPKeystoreBinder {

    @Autowired
    private EntityFactory entityFactory;

    private SAML2SPKeystore getSAML2SPKeystore(
        final SAML2SPKeystore keystore,
        final SAML2SPKeystoreTO keystoreTO) {

        SAML2SPKeystore result = keystore;
        if (result == null) {
            result = entityFactory.newEntity(SAML2SPKeystore.class);
        }
        result.setKeystore(keystoreTO.getKeystore());
        result.setOwner(keystoreTO.getOwner());

        return result;
    }

    @Override
    public SAML2SPKeystore create(final SAML2SPKeystoreTO keystoreTO) {
        return update(entityFactory.newEntity(SAML2SPKeystore.class), keystoreTO);
    }

    @Override
    public SAML2SPKeystore update(
        final SAML2SPKeystore keystore,
        final SAML2SPKeystoreTO keystoreTO) {

        return getSAML2SPKeystore(keystore, keystoreTO);
    }

    @Override
    public SAML2SPKeystoreTO getSAML2SPKeystoreTO(final SAML2SPKeystore keystore) {
        SAML2SPKeystoreTO keystoreTO = new SAML2SPKeystoreTO();

        keystoreTO.setKey(keystore.getKey());
        keystoreTO.setKeystore(keystore.getKeystore());
        keystoreTO.setOwner(keystore.getOwner());

        return keystoreTO;
    }
}
