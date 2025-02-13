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
package org.apache.syncope.core.persistence.neo4j.entity.user;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.entity.UMembershipType;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractMembership;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jUMembership.NODE)
public class Neo4jUMembership extends AbstractMembership<User> implements UMembership {

    private static final long serialVersionUID = -14584450896965100L;

    public static final String NODE = "UMembership";

    @Relationship(type = Neo4jUser.USER_GROUP_MEMBERSHIP_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jUser leftEnd;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jGroup rightEnd;

    @CompositeProperty(converterRef = "plainAttrsConverter")
    protected Map<String, PlainAttr> plainAttrs = new HashMap<>();

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
        checkType(leftEnd, Neo4jUser.class);
        this.leftEnd = (Neo4jUser) leftEnd;
    }

    @Override
    public Group getRightEnd() {
        return rightEnd;
    }

    @Override
    public void setRightEnd(final Group rightEnd) {
        checkType(rightEnd, Neo4jGroup.class);
        this.rightEnd = (Neo4jGroup) rightEnd;
    }

    @Override
    protected Map<String, PlainAttr> plainAttrs() {
        return plainAttrs;
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return plainAttrs.entrySet().stream().
                filter(e -> e.getValue() != null).
                sorted(Comparator.comparing(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
    }

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return Optional.ofNullable(plainAttrs.get(plainSchema));
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return getKey().equals(attr.getMembership())
                && plainAttrs.put(attr.getSchema(), attr) != null;
    }

    @Override
    public boolean remove(final String plainSchema) {
        return plainAttrs.put(plainSchema, null) != null;
    }
}
