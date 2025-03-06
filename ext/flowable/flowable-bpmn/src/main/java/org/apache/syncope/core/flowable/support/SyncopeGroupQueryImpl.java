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

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.GroupQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class SyncopeGroupQueryImpl extends GroupQueryImpl {

    private static final long serialVersionUID = -2595069675443343682L;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    private List<Group> result;

    private static Group fromSyncopeGroup(final String name) {
        GroupEntity group = new GroupEntityImpl();
        group.setId(name);
        return group;
    }

    private void execute() {
        if (id != null) {
            result = groupDAO.findByName(id).
                    map(group -> List.of(fromSyncopeGroup(group.getName()))).
                orElseGet(List::of);
        } else if (userId != null) {
            result = userDAO.findByUsername(userId).
                    map(user -> userDAO.findAllGroupNames(user).stream().
                    map(SyncopeGroupQueryImpl::fromSyncopeGroup).
                    toList()).
                orElseGet(List::of);
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
    public List<Group> list() {
        checkQueryOk();

        this.resultType = ResultType.LIST;
        if (result == null) {
            execute();
        }
        return result;
    }
}
