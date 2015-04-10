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

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleDataBinderImpl implements RoleDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(RoleDataBinder.class);

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Role create(final RoleTO roleTO) {
        Role role = entityFactory.newEntity(Role.class);
        update(role, roleTO);
        return role;
    }

    @Override
    public void update(final Role role, final RoleTO roleTO) {
        role.setName(roleTO.getName());
        role.setCriteria(roleTO.getCriteria());

        role.getEntitlements().clear();
        role.getEntitlements().addAll(roleTO.getEntitlements());

        role.getRealms().clear();
        CollectionUtils.forAllDo(roleTO.getRealms(), new Closure<String>() {

            @Override
            public void execute(final String realmFullPath) {
                Realm realm = realmDAO.find(realmFullPath);
                if (realm == null) {
                    LOG.warn("Invalid realm full path {}, ignoring", realmFullPath);
                } else {
                    role.addRealm(realm);
                }
            }
        });
    }

    @Override
    public RoleTO getRoleTO(final Role role) {
        RoleTO roleTO = new RoleTO();

        roleTO.setKey(role.getKey());
        roleTO.setName(role.getName());
        roleTO.setCriteria(role.getCriteria());
        roleTO.getEntitlements().addAll(role.getEntitlements());

        CollectionUtils.collect(role.getRealms(), new Transformer<Realm, String>() {

            @Override
            public String transform(final Realm input) {
                return input.getFullPath();
            }
        }, roleTO.getRealms());

        return roleTO;
    }

}
