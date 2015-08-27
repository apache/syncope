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

import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RealmDataBinderImpl implements RealmDataBinder {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Realm create(final String parentPath, final RealmTO realmTO) {
        Realm realm = entityFactory.newEntity(Realm.class);

        realm.setName(realmTO.getName());
        realm.setParent(realmDAO.find(parentPath));

        if (realmTO.getPasswordPolicy() != null) {
            realm.setPasswordPolicy((PasswordPolicy) policyDAO.find(realmTO.getPasswordPolicy()));
        }
        if (realmTO.getAccountPolicy() != null) {
            realm.setAccountPolicy((AccountPolicy) policyDAO.find(realmTO.getAccountPolicy()));
        }

        return realm;
    }

    @Override
    public void update(final Realm realm, final RealmTO realmTO) {
        realm.setName(realmTO.getName());
        realm.setParent(realmTO.getParent() == 0 ? null : realmDAO.find(realmTO.getParent()));

        if (realmTO.getPasswordPolicy() != null) {
            realm.setPasswordPolicy((PasswordPolicy) policyDAO.find(realmTO.getPasswordPolicy()));
        }
        if (realmTO.getAccountPolicy() != null) {
            realm.setAccountPolicy((AccountPolicy) policyDAO.find(realmTO.getAccountPolicy()));
        }
    }

    @Override
    public RealmTO getRealmTO(final Realm realm) {
        RealmTO realmTO = new RealmTO();

        realmTO.setKey(realm.getKey());
        realmTO.setName(realm.getName());
        realmTO.setParent(realm.getParent() == null ? 0 : realm.getParent().getKey());
        realmTO.setFullPath(realm.getFullPath());
        realmTO.setAccountPolicy(realm.getAccountPolicy() == null ? null : realm.getAccountPolicy().getKey());
        realmTO.setPasswordPolicy(realm.getPasswordPolicy() == null ? null : realm.getPasswordPolicy().getKey());

        return realmTO;
    }

}
