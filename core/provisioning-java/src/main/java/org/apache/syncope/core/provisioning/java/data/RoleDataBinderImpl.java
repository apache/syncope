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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
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

    private void setDynMembership(final Role role, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
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
        Role role = entityFactory.newEntity(Role.class);
        update(role, roleTO);
        return role;
    }

    @Override
    public void update(final Role role, final RoleTO roleTO) {
        role.setKey(roleTO.getKey());

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

        // dynamic membership
        if (role.getKey() == null && roleTO.getDynMembershipCond() != null) {
            setDynMembership(role, roleTO.getDynMembershipCond());
        } else if (role.getDynMembership() != null && roleTO.getDynMembershipCond() == null) {
            role.setDynMembership(null);
        } else if (role.getDynMembership() == null && roleTO.getDynMembershipCond() != null) {
            setDynMembership(role, roleTO.getDynMembershipCond());
        } else if (role.getDynMembership() != null && roleTO.getDynMembershipCond() != null
                && !role.getDynMembership().getFIQLCond().equals(roleTO.getDynMembershipCond())) {

            role.getDynMembership().getMembers().clear();
            setDynMembership(role, roleTO.getDynMembershipCond());
        }
    }

    @Override
    public RoleTO getRoleTO(final Role role) {
        RoleTO roleTO = new RoleTO();

        roleTO.setKey(role.getKey());
        roleTO.getEntitlements().addAll(role.getEntitlements());

        CollectionUtils.collect(role.getRealms(), new Transformer<Realm, String>() {

            @Override
            public String transform(final Realm input) {
                return input.getFullPath();
            }
        }, roleTO.getRealms());

        if (role.getDynMembership() != null) {
            roleTO.setDynMembershipCond(role.getDynMembership().getFIQLCond());
        }

        return roleTO;
    }

}
