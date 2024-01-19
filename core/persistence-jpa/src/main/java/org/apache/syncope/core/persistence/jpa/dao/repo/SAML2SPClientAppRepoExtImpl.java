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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.jpa.entity.am.JPASAML2SPClientApp;

public class SAML2SPClientAppRepoExtImpl
        extends AbstractClientRepoExt<SAML2SPClientApp>
        implements SAML2SPClientAppRepoExt {

    public SAML2SPClientAppRepoExtImpl(final EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public List<SAML2SPClientApp> findAllByPolicy(final Policy policy) {
        return findAllByPolicy(policy, SAML2SPClientApp.class, JPASAML2SPClientApp.class);
    }

    @Override
    public List<SAML2SPClientApp> findAllByRealm(final Realm realm) {
        return findAllByRealm(realm, SAML2SPClientApp.class, JPASAML2SPClientApp.class);
    }

    @Override
    public SAML2SPClientApp save(final SAML2SPClientApp clientApp) {
        ((JPASAML2SPClientApp) clientApp).list2json();
        return entityManager.merge(clientApp);
    }
}
