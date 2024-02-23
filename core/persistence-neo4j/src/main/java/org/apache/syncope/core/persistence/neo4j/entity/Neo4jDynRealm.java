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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.common.validation.DynRealmCheck;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jDynRealm.NODE)
@DynRealmCheck
public class Neo4jDynRealm extends AbstractProvidedKeyNode implements DynRealm {

    private static final long serialVersionUID = -6851035842423560341L;

    public static final String NODE = "DynRealm";

    public static final String DYNREALM_MEMBERSHIP_REL = "DYNREALM_MEMBERSHIP";

    @Relationship(type = DYNREALM_MEMBERSHIP_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jDynRealmMembership> dynMemberships = new ArrayList<>();

    @Override
    public boolean add(final DynRealmMembership dynRealmMembership) {
        checkType(dynRealmMembership, Neo4jDynRealmMembership.class);
        return this.dynMemberships.add((Neo4jDynRealmMembership) dynRealmMembership);
    }

    @Override
    public Optional<? extends DynRealmMembership> getDynMembership(final AnyType anyType) {
        return dynMemberships.stream().
                filter(dynRealmMembership -> anyType != null && anyType.equals(dynRealmMembership.getAnyType())).
                findFirst();
    }

    @Override
    public List<? extends DynRealmMembership> getDynMemberships() {
        return dynMemberships;
    }
}
