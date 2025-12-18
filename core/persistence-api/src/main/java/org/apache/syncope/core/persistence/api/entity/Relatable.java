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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;

public interface Relatable<L extends Any, R extends Relationship<L, AnyObject>> extends Any {

    /**
     * Returns the plain attribute for this instance, the given schema name and the given relationship.
     *
     * @param plainSchema plain schema name
     * @param relationship relationship
     * @return plain attribute for this instance, the given schema name and the given membership
     */
    Optional<PlainAttr> getPlainAttr(String plainSchema, Relationship<?, ?> relationship);

    /**
     * Returns the list of plain attributes for this instance and the given membership.
     *
     * @param relationship membership
     * @return list of plain attributes for this instance and the given membership
     */
    List<PlainAttr> getPlainAttrs(Relationship<?, ?> relationship);

    boolean add(R relationship);

    boolean remove(Relationship<?, ?> relationship);

    Optional<? extends R> getRelationship(RelationshipType relationshipType, String otherEndKey);

    List<? extends R> getRelationships(String otherEndKey);

    List<? extends R> getRelationships(RelationshipType relationshipType);

    List<? extends R> getRelationships();
}
