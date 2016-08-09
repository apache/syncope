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

import java.util.Collection;
import java.util.List;

public interface GroupableRelatable<
        L extends Any<P>, 
        M extends Membership<L>, 
        P extends GroupablePlainAttr<L, M>,
        R extends Any<?>,
        REL extends Relationship<L, R>> extends Any<P> {

    @Override
    boolean add(final P attr);

    @Override
    boolean remove(final P attr);

    /**
     * Returns the plain attribute for this instance and the given schema name - if found, {@code NULL} otherwise.
     * <b>IMPORTANT:</b> This method won't return any attribute related to memberships.
     *
     * @param plainSchemaName plain schema name
     * @return plain attribute for this instance and the given schema name - if found, {@code NULL} otherwise
     */
    @Override
    P getPlainAttr(String plainSchemaName);

    /**
     * Returns the plain attribute for this instance, the given schema name and the given membership -
     * if found, {@code NULL} otherwise.
     *
     * @param plainSchemaName plain schema name
     * @param membership membership
     * @return plain attribute for this instance, the given schema name and the given membership -
     * if found, {@code NULL} otherwise
     */
    P getPlainAttr(String plainSchemaName, Membership<?> membership);

    /**
     * Returns the plain attributes for this instance.
     * <b>IMPORTANT:</b> This method won't return any attribute related to memberships.
     *
     * @return plain attribute for this instance
     */
    @Override
    List<? extends P> getPlainAttrs();

    /**
     * Returns the list of plain attributes for this instance and the given schema name (including membeship attributes,
     * as opposite to {@link Any#getPlainAttr(java.lang.String)}).
     *
     * @param plainSchemaName plain schema name
     * @return list of plain attributes for this instance and the given schema name (including membeship attributes)
     */
    Collection<? extends P> getPlainAttrs(String plainSchemaName);

    /**
     * Returns the list of plain attributes for this instance and the given membership.
     *
     * @param membership membership
     * @return list of plain attributes for this instance and the given membership
     */
    Collection<? extends P> getPlainAttrs(Membership<?> membership);

    boolean add(M membership);

    M getMembership(String groupKey);

    List<? extends M> getMemberships();

    boolean add(REL relationship);

    REL getRelationship(RelationshipType relationshipType, String otherEndKey);

    Collection<? extends REL> getRelationships(String otherEndKey);

    Collection<? extends REL> getRelationships(RelationshipType relationshipType);

    List<? extends REL> getRelationships();

}
