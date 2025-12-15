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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.RelationshipTypeExtension;

@Entity
@Table(name = JPARelationshipTypeExtension.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "relationshipType_id", "anyType_id" }))
public class JPARelationshipTypeExtension extends AbstractTypeExtension implements RelationshipTypeExtension {

    private static final long serialVersionUID = -8367626793791263551L;

    public static final String TABLE = "RelationshipTypeExtension";

    @ManyToOne
    private JPARelationshipType relationshipType;

    @Override
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    @Override
    public void setRelationshipType(final RelationshipType relationshipType) {
        checkType(relationshipType, JPARelationshipType.class);
        this.relationshipType = (JPARelationshipType) relationshipType;
    }
}
