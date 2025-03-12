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
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.entity.AMembershipType;
import org.apache.syncope.core.persistence.common.entity.UMembershipType;
import org.apache.syncope.core.persistence.common.validation.RelationshipCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPARelationshipType;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;

@Entity
@Table(name = JPAURelationship.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "type_id", "user_id", "anyObject_id" }))
@RelationshipCheck
public class JPAURelationship extends AbstractGeneratedKeyEntity implements URelationship {

    private static final long serialVersionUID = 2778494939240083204L;

    public static final String TABLE = "URelationship";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARelationshipType type;

    @ManyToOne
    @Column(name = "user_id")
    private JPAUser leftEnd;

    @ManyToOne
    @Column(name = "anyObject_id")
    private JPAAnyObject rightEnd;

    @Override
    public RelationshipType getType() {
        return type;
    }

    @Override
    public void setType(final RelationshipType type) {
        if (UMembershipType.KEY.equalsIgnoreCase(type.getKey())
                || AMembershipType.KEY.equalsIgnoreCase(type.getKey())) {

            throw new IllegalArgumentException("This is not a membership");
        }
        checkType(type, JPARelationshipType.class);
        this.type = (JPARelationshipType) type;
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
    public AnyObject getRightEnd() {
        return rightEnd;
    }

    @Override
    public void setRightEnd(final AnyObject rightEnd) {
        checkType(rightEnd, JPAAnyObject.class);
        this.rightEnd = (JPAAnyObject) rightEnd;
    }
}
