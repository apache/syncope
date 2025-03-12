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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.entity.UMembershipType;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;

@Entity
@Table(name = JPAUMembership.TABLE)
public class JPAUMembership extends AbstractGeneratedKeyEntity implements UMembership {

    private static final long serialVersionUID = -14584450896965100L;

    public static final String TABLE = "UMembership";

    @ManyToOne
    @Column(name = "user_id")
    private JPAUser leftEnd;

    @ManyToOne
    @Column(name = "group_id")
    private JPAGroup rightEnd;

    @Override
    public UMembershipType getType() {
        return UMembershipType.getInstance();
    }

    @Override
    public void setType(final RelationshipType type) {
        // cannot be changed
    }

    @Override
    public User getLeftEnd() {
        return leftEnd;
    }

    @Override
    public void setLeftEnd(final User leftEnd) {
        checkType(leftEnd, JPAUser.class);
        this.leftEnd = (JPAUser) leftEnd;
    }

    @Override
    public Group getRightEnd() {
        return rightEnd;
    }

    @Override
    public void setRightEnd(final Group rightEnd) {
        checkType(rightEnd, JPAGroup.class);
        this.rightEnd = (JPAGroup) rightEnd;
    }
}
