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
package org.apache.syncope.core.persistence.neo4j.entity.anyobject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.common.entity.AMembershipType;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractMembership;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAMembership.NODE)
public class Neo4jAMembership extends AbstractMembership<AnyObject> implements AMembership {

    private static final long serialVersionUID = -14584450896965100L;

    public static final String NODE = "AMembership";

    @Relationship(type = Neo4jAnyObject.ANY_OBJECT_GROUP_MEMBERSHIP_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyObject leftEnd;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jGroup rightEnd;

    @CompositeProperty(converterRef = "plainAttrsConverter")
    private Map<String, PlainAttr> plainAttrs = new HashMap<>();

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
        checkType(leftEnd, Neo4jAnyObject.class);
        this.leftEnd = (Neo4jAnyObject) leftEnd;
        this.aMembershipType = Optional.ofNullable(leftEnd).map(le -> new AMembershipType(le.getType())).orElse(null);
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

    @PostLoad
    public void postLoad() {
        this.aMembershipType = new AMembershipType(leftEnd.getType());
    }
}
