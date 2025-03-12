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
package org.apache.syncope.core.flowable.support;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.UserQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class SyncopeUserQueryImpl extends UserQueryImpl {

    private static final long serialVersionUID = 4403344392227706318L;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    private List<User> result;

    private static User fromSyncopeUser(final org.apache.syncope.core.persistence.api.entity.user.User syncopeUser) {
        UserEntity user = new UserEntityImpl();
        user.setId(syncopeUser.getUsername());
        return user;
    }

    private void execute() {
        if (id != null) {
            result = userDAO.findByUsername(id).
                    filter(user -> groupId == null || userDAO.findAllGroupNames(user).contains(groupId)).
                    map(user -> List.of(fromSyncopeUser(user))).
                orElseGet(List::of);
        } else if (groupId != null) {
            groupDAO.findByName(groupId).map(group -> {
                List<User> r = new ArrayList<>();
                groupDAO.findUMemberships(group).stream().map(m -> fromSyncopeUser(m.getLeftEnd())).
                        filter(user -> !r.contains(user)).
                        forEach(r::add);
                return r;
            }).orElseGet(List::of);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        checkQueryOk();

        this.resultType = ResultType.COUNT;
        if (result == null) {
            execute();
        }
        return result.size();
    }

    @Transactional(readOnly = true)
    @Override
    public List<User> list() {
        checkQueryOk();

        this.resultType = ResultType.LIST;
        if (result == null) {
            execute();
        }
        return result;
    }

}
