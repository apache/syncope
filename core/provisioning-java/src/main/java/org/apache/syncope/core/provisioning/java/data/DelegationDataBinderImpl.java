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

import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegationDataBinderImpl implements DelegationDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(DelegationDataBinder.class);

    protected final UserDAO userDAO;

    protected final RoleDAO roleDAO;

    protected final EntityFactory entityFactory;

    public DelegationDataBinderImpl(
            final UserDAO userDAO,
            final RoleDAO roleDAO,
            final EntityFactory entityFactory) {

        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public Delegation create(final DelegationTO delegationTO) {
        Delegation delegation = entityFactory.newEntity(Delegation.class);

        User delegating = userDAO.findById(delegationTO.getDelegating()).
                orElseThrow(() -> new NotFoundException("Delegating User " + delegationTO.getDelegating()));
        delegation.setDelegating(delegating);

        User delegated = userDAO.findById(delegationTO.getDelegated()).
                orElseThrow(() -> new NotFoundException("Delegated User " + delegationTO.getDelegated()));
        delegation.setDelegated(delegated);

        return update(delegation, delegationTO);
    }

    @Override
    public Delegation update(final Delegation delegation, final DelegationTO delegationTO) {
        delegation.setStart(delegationTO.getStart());
        delegation.setEnd(delegationTO.getEnd());

        // 1. add or update all (valid) roles from TO
        delegationTO.getRoles().forEach(roleTO -> {
            if (roleTO == null) {
                LOG.error("Null {}", RoleTO.class.getSimpleName());
            } else {
                Role role = roleDAO.findById(roleTO).
                        orElseThrow(() -> {
                            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRole);
                            sce.getElements().add("Role " + roleTO + " not found");
                            return sce;
                        });

                delegation.add(role);
            }
        });

        // 2. remove all roles not contained in the TO
        for (Iterator<? extends Role> itor = delegation.getRoles().iterator(); itor.hasNext();) {
            Role role = itor.next();
            if (delegationTO.getRoles().stream().noneMatch(roleKey -> roleKey.equals(role.getKey()))) {
                itor.remove();
            }
        }

        return delegation;
    }

    @Override
    public DelegationTO getDelegationTO(final Delegation delegation) {
        DelegationTO delegationTO = new DelegationTO();

        delegationTO.setKey(delegation.getKey());
        delegationTO.setDelegating(delegation.getDelegating().getKey());
        delegationTO.setDelegated(delegation.getDelegated().getKey());
        delegationTO.setStart(delegation.getStart());
        delegationTO.setEnd(delegation.getEnd());
        delegationTO.getRoles().addAll(delegation.getRoles().stream().map(Role::getKey).collect(Collectors.toSet()));

        return delegationTO;
    }
}
