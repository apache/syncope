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

import java.util.function.BiFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class Neo4jImplementationRelationship
        extends Neo4jSortedRelationsihip<Neo4jImplementation>
        implements Comparable<Neo4jImplementationRelationship> {

    public static BiFunction<Integer, Neo4jImplementation, Neo4jImplementationRelationship> builder() {
        return Neo4jImplementationRelationship::new;
    }

    @RelationshipId
    private Long id;

    private int index;

    @TargetNode
    private Neo4jImplementation implementation;

    public Neo4jImplementationRelationship(final int index, final Neo4jImplementation implementation) {
        this.index = index;
        this.implementation = implementation;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Neo4jImplementation getEntity() {
        return implementation;
    }

    @Override
    public int compareTo(final Neo4jImplementationRelationship object) {
        return Integer.compare(index, object.getIndex());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Neo4jImplementationRelationship other = (Neo4jImplementationRelationship) obj;
        return new EqualsBuilder().
                append(index, other.index).
                append(implementation, other.implementation).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(index).
                append(implementation).
                build();
    }

    @Override
    public String toString() {
        return "Neo4jImplementationRelationship{"
                + "id=" + id
                + ", index=" + index
                + ", implementation=" + implementation
                + '}';
    }
}
