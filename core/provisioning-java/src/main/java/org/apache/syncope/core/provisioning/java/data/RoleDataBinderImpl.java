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

import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleDataBinderImpl implements RoleDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(RoleDataBinder.class);

    protected final RealmDAO realmDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final RoleDAO roleDAO;

    protected final ApplicationDAO applicationDAO;

    protected final EntityFactory entityFactory;

    protected final SearchCondVisitor searchCondVisitor;

    public RoleDataBinderImpl(
            final RealmDAO realmDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final ApplicationDAO applicationDAO,
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor) {

        this.realmDAO = realmDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.roleDAO = roleDAO;
        this.applicationDAO = applicationDAO;
        this.entityFactory = entityFactory;
        this.searchCondVisitor = searchCondVisitor;
    }

    protected void setDynMembership(final Role role, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(searchCondVisitor, dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }

        DynRoleMembership dynMembership;
        if (role.getDynMembership() == null) {
            dynMembership = entityFactory.newEntity(DynRoleMembership.class);
            dynMembership.setRole(role);
            role.setDynMembership(dynMembership);
        } else {
            dynMembership = role.getDynMembership();
        }
        dynMembership.setFIQLCond(dynMembershipFIQL);
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
            Realm realm = realmDAO.findByFullPath(realmFullPath);
            if (realm == null) {
                LOG.debug("Invalid realm full path {}, ignoring", realmFullPath);
            } else {
                role.add(realm);
            }
        }

        role.getDynRealms().clear();
        for (String key : roleTO.getDynRealms()) {
            DynRealm dynRealm = dynRealmDAO.find(key);
            if (dynRealm == null) {
                LOG.debug("Invalid dynamic ream {}, ignoring", key);
            } else {
                role.add(dynRealm);
            }
        }

        role = roleDAO.save(role);

        // dynamic membership
        roleDAO.clearDynMembers(role);
        if (role.getKey() == null && roleTO.getDynMembershipCond() != null) {
            setDynMembership(role, roleTO.getDynMembershipCond());
        } else if (role.getDynMembership() != null && roleTO.getDynMembershipCond() == null) {
            role.setDynMembership(null);
        } else if (role.getDynMembership() == null && roleTO.getDynMembershipCond() != null) {
            setDynMembership(role, roleTO.getDynMembershipCond());
        } else if (role.getDynMembership() != null && roleTO.getDynMembershipCond() != null
                && !role.getDynMembership().getFIQLCond().equals(roleTO.getDynMembershipCond())) {

            setDynMembership(role, roleTO.getDynMembershipCond());
        }

        role.getPrivileges().clear();
        for (String key : roleTO.getPrivileges()) {
            Privilege privilege = applicationDAO.findPrivilege(key);
            if (privilege == null) {
                LOG.debug("Invalid privilege {}, ignoring", key);
            } else {
                role.add(privilege);
            }
        }

        return roleDAO.saveAndRefreshDynMemberships(role);
    }

    @Override
    public RoleTO getRoleTO(final Role role) {
        RoleTO roleTO = new RoleTO();

        roleTO.setKey(role.getKey());
        roleTO.getEntitlements().addAll(role.getEntitlements());

        roleTO.getRealms().addAll(role.getRealms().stream().
                map(Realm::getFullPath).collect(Collectors.toList()));

        roleTO.getDynRealms().addAll(role.getDynRealms().stream().
                map(Entity::getKey).collect(Collectors.toList()));

        if (role.getDynMembership() != null) {
            roleTO.setDynMembershipCond(role.getDynMembership().getFIQLCond());
        }

        roleTO.getPrivileges().addAll(role.getPrivileges().stream().
                map(Entity::getKey).collect(Collectors.toList()));

        return roleTO;
    }
}
