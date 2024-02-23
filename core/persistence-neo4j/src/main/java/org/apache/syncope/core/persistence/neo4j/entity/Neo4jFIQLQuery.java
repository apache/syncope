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
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jFIQLQuery.NODE)
public class Neo4jFIQLQuery extends AbstractGeneratedKeyNode implements FIQLQuery {

    private static final long serialVersionUID = -8800817340585235280L;

    public static final String NODE = "FIQLQuery";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jUser owner;

    @NotNull
    private String name;

    @NotNull
    private String target;

    @NotNull
    private String fiql;

    @Override
    public Neo4jUser getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, Neo4jUser.class);
        this.owner = (Neo4jUser) owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void setTarget(final String target) {
        this.target = target;
    }

    @Override
    public String getFIQL() {
        return fiql;
    }

    @Override
    public void setFIQL(final String fiql) {
        this.fiql = fiql;
    }
}
