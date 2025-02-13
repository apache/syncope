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

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.common.validation.RelationshipTypeCheck;

@Entity
@Table(name = JPARelationshipType.TABLE)
@RelationshipTypeCheck
@Cacheable
public class JPARelationshipType extends AbstractProvidedKeyEntity implements RelationshipType {

    private static final long serialVersionUID = -753673974614737065L;

    public static final String TABLE = "RelationshipType";

    private String description;

    @NotNull
    @ManyToOne
    private JPAAnyType leftEndAnyType;

    @NotNull
    @ManyToOne
    private JPAAnyType rightEndAnyType;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public AnyType getLeftEndAnyType() {
        return leftEndAnyType;
    }

    @Override
    public void setLeftEndAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.leftEndAnyType = (JPAAnyType) anyType;
    }

    @Override
    public AnyType getRightEndAnyType() {
        return rightEndAnyType;
    }

    @Override
    public void setRightEndAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.rightEndAnyType = (JPAAnyType) anyType;
    }
}
