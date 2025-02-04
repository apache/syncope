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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleDataBinderImpl implements RoleDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(RoleDataBinder.class);

    protected final RealmSearchDAO realmSearchDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final RoleDAO roleDAO;

    protected final EntityFactory entityFactory;

    protected final SearchCondVisitor searchCondVisitor;

    public RoleDataBinderImpl(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor) {

        this.realmSearchDAO = realmSearchDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.roleDAO = roleDAO;
        this.entityFactory = entityFactory;
        this.searchCondVisitor = searchCondVisitor;
    }

    @Override
    public Role create(final RoleTO roleTO) {
        return update(entityFactory.newEntity(Role.class), roleTO);
    }

    @Override
    public Role update(final Role toBeUpdated, final RoleTO roleTO) {
        toBeUpdated.setKey(roleTO.getKey());
        Role role = roleDAO.save(toBeUpdated);

        role.getEntitlements().clear();
        role.getEntitlements().addAll(roleTO.getEntitlements());

        role.getRealms().clear();
        for (String realmFullPath : roleTO.getRealms()) {
            realmSearchDAO.findByFullPath(realmFullPath).ifPresentOrElse(
                    role::add,
                    () -> LOG.debug("Invalid realm full path {}, ignoring", realmFullPath));
        }

        role.getDynRealms().clear();
        for (String key : roleTO.getDynRealms()) {
            dynRealmDAO.findById(key).ifPresentOrElse(
                    role::add,
                    () -> LOG.debug("Invalid dynamic ream {}, ignoring", key));
        }

        role = roleDAO.save(role);

        // dynamic membership
        roleDAO.clearDynMembers(role);
        if (roleTO.getDynMembershipCond() == null) {
            role.setDynMembershipCond(null);
        } else {
            if (!SearchCondConverter.convert(searchCondVisitor, roleTO.getDynMembershipCond()).isValid()) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                sce.getElements().add(roleTO.getDynMembershipCond());
                throw sce;
            }

            role.setDynMembershipCond(roleTO.getDynMembershipCond());
        }

        return roleDAO.saveAndRefreshDynMemberships(role);
    }

    @Override
    public RoleTO getRoleTO(final Role role) {
        RoleTO roleTO = new RoleTO();

        roleTO.setKey(role.getKey());
        roleTO.getEntitlements().addAll(role.getEntitlements());

        roleTO.getRealms().addAll(role.getRealms().stream().map(Realm::getFullPath).toList());

        roleTO.getDynRealms().addAll(role.getDynRealms().stream().map(DynRealm::getKey).toList());

        roleTO.setDynMembershipCond(role.getDynMembershipCond());

        return roleTO;
    }
}
