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
package org.apache.syncope.core.persistence.jpa.entity.user;

import java.util.List;
import javax.persistence.MappedSuperclass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDynMembership;

@MappedSuperclass
public abstract class AbstractUDynMembership extends AbstractDynMembership<User> {

    private static final long serialVersionUID = 6296230283800203205L;

    protected abstract List<JPAUser> internalGetUsers();

    @Override
    public boolean add(final User user) {
        checkType(user, JPAUser.class);
        return internalGetUsers().add((JPAUser) user);
    }

    @Override
    public List<? extends User> getMembers() {
        return internalGetUsers();
    }
}
