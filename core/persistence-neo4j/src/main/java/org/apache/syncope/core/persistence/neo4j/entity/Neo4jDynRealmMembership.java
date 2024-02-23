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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jDynRealmMembership.NODE)
public class Neo4jDynRealmMembership extends AbstractGeneratedKeyNode implements DynRealmMembership {

    private static final long serialVersionUID = 8157856850557493134L;

    public static final String NODE = "DynRealmMembership";

    @NotNull
    private String fiql;

    @Relationship(type = Neo4jDynRealm.DYNREALM_MEMBERSHIP_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jDynRealm dynRealm;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyType anyType;

    @Override
    public String getFIQLCond() {
        return fiql;
    }

    @Override
    public void setFIQLCond(final String fiql) {
        this.fiql = fiql;
    }

    @Override
    public DynRealm getDynRealm() {
        return dynRealm;
    }

    @Override
    public void setDynRealm(final DynRealm dynRealm) {
        checkType(dynRealm, Neo4jDynRealm.class);
        this.dynRealm = (Neo4jDynRealm) dynRealm;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, Neo4jAnyType.class);
        this.anyType = (Neo4jAnyType) anyType;
    }
}
