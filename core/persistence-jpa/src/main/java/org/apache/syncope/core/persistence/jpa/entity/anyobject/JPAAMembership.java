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
package org.apache.syncope.core.persistence.jpa.entity.anyobject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.common.entity.AMembershipType;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;

@Entity
@Table(name = JPAAMembership.TABLE)
public class JPAAMembership extends AbstractGeneratedKeyEntity implements AMembership {

    private static final long serialVersionUID = 1503557547394601405L;

    public static final String TABLE = "AMembership";

    @ManyToOne
    @Column(name = "anyObject_id")
    private JPAAnyObject leftEnd;

    @ManyToOne
    @Column(name = "group_id")
    private JPAGroup rightEnd;

    @Transient
    private AMembershipType aMembershipType;

    @Override
    public RelationshipType getType() {
        return aMembershipType;
    }

    @Override
    public void setType(final RelationshipType type) {
        // cannot be changed
    }

    @Override
    public AnyObject getLeftEnd() {
        return leftEnd;
    }

    @Override
    public void setLeftEnd(final AnyObject leftEnd) {
        checkType(leftEnd, JPAAnyObject.class);
        this.leftEnd = (JPAAnyObject) leftEnd;
        this.aMembershipType = Optional.ofNullable(leftEnd).map(le -> new AMembershipType(le.getType())).orElse(null);
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

    @PostLoad
    public void postLoad() {
        this.aMembershipType = new AMembershipType(leftEnd.getType());
    }
}
