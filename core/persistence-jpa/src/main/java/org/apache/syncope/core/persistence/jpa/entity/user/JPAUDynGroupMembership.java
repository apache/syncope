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

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDynMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;

@Entity
@Table(name = JPAUDynGroupMembership.TABLE)
public class JPAUDynGroupMembership extends AbstractDynMembership<User> implements UDynGroupMembership {

    private static final long serialVersionUID = -7336814163949640354L;

    public static final String TABLE = "UDynGroupMembership";

    @OneToOne
    private JPAGroup group;

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(final Group group) {
        checkType(group, JPAGroup.class);
        this.group = (JPAGroup) group;
    }
}
