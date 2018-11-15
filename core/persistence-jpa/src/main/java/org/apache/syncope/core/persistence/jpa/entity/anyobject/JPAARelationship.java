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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.MembershipType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPARelationshipType;

@Entity
@Table(name = JPAARelationship.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "type_id", "left_anyObject_id", "right_anyObject_id" }))
public class JPAARelationship extends AbstractGeneratedKeyEntity implements ARelationship {

    private static final long serialVersionUID = 6608821135023815357L;

    public static final String TABLE = "ARelationship";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARelationshipType type;

    private AnyObject leftEnd;

    private AnyObject rightEnd;

    @Override
    public RelationshipType getType() {
        return type;
    }

    @Override
    public void setType(final RelationshipType type) {
        if (MembershipType.getInstance().getKey().equalsIgnoreCase(type.getKey())) {
            throw new IllegalArgumentException("This is not a membership");
        }
        checkType(type, JPARelationshipType.class);
        this.type = (JPARelationshipType) type;
    }

    @Override
    public AnyObject getLeftEnd() {
        return leftEnd;
    }

    @Override
    public void setLeftEnd(final AnyObject leftEnd) {
        checkType(leftEnd, JPAAnyObject.class);
        this.leftEnd = (JPAAnyObject) leftEnd;
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
