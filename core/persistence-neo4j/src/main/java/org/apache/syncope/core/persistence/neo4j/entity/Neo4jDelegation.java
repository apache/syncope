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
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.validation.DelegationCheck;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jDelegation.NODE)
@DelegationCheck
public class Neo4jDelegation extends AbstractGeneratedKeyNode implements Delegation {

    private static final long serialVersionUID = 17988340419552L;

    public static final String NODE = "Delegation";

    public static final String DELEGATING_REL = "DELEGATING";

    public static final String DELEGATED_REL = "DELEGATED";

    @NotNull
    @Relationship(type = DELEGATING_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jUser delegating;

    @NotNull
    @Relationship(type = DELEGATED_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jUser delegated;

    @NotNull
    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Set<Neo4jRole> roles = new HashSet<>();

    @Override
    public User getDelegating() {
        return delegating;
    }

    @Override
    public void setDelegating(final User delegating) {
        checkType(delegating, Neo4jUser.class);
        this.delegating = (Neo4jUser) delegating;
    }

    @Override
    public User getDelegated() {
        return delegated;
    }

    @Override
    public void setDelegated(final User delegated) {
        checkType(delegated, Neo4jUser.class);
        this.delegated = (Neo4jUser) delegated;
    }

    @Override
    public OffsetDateTime getStart() {
        return startDate;
    }

    @Override
    public void setStart(final OffsetDateTime start) {
        this.startDate = start;
    }

    @Override
    public OffsetDateTime getEnd() {
        return endDate;
    }

    @Override
    public void setEnd(final OffsetDateTime end) {
        this.endDate = end;
    }

    @Override
    public boolean add(final Role role) {
        checkType(role, Neo4jRole.class);
        return roles.add((Neo4jRole) role);
    }

    @Override
    public Set<? extends Role> getRoles() {
        return roles;
    }
}
