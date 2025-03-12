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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import java.util.function.BiFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSortedRelationsihip;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class Neo4jMacroTaskCommandRelationship
        extends Neo4jSortedRelationsihip<Neo4jMacroTaskCommand>
        implements Comparable<Neo4jMacroTaskCommandRelationship> {

    public static BiFunction<Integer, Neo4jMacroTaskCommand, Neo4jMacroTaskCommandRelationship> builder() {
        return Neo4jMacroTaskCommandRelationship::new;
    }

    @RelationshipId
    private Long id;

    private int index;

    @TargetNode
    private Neo4jMacroTaskCommand command;

    public Neo4jMacroTaskCommandRelationship(final int index, final Neo4jMacroTaskCommand command) {
        this.index = index;
        this.command = command;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Neo4jMacroTaskCommand getEntity() {
        return command;
    }

    @Override
    public int compareTo(final Neo4jMacroTaskCommandRelationship object) {
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
        Neo4jMacroTaskCommandRelationship other = (Neo4jMacroTaskCommandRelationship) obj;
        return new EqualsBuilder().
                append(index, other.index).
                append(command, other.command).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(index).
                append(command).
                build();
    }

    @Override
    public String toString() {
        return "Neo4jMacroTaskCommandRelationship{"
                + "id=" + id
                + ", index=" + index
                + ", command=" + command
                + '}';
    }
}
