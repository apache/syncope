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
import java.util.Optional;

public interface Groupable<L extends Any, M extends Membership<L>, R extends Any, REL extends Relationship<L, R>>
        extends Any {

    /**
     * Returns the plain attribute for this instance, the given schema name and the given membership -
     * if found, {@code NULL} otherwise.
     *
     * @param plainSchema plain schema name
     * @param membership membership
     * @return plain attribute for this instance, the given schema name and the given membership
     */
    Optional<PlainAttr> getPlainAttr(String plainSchema, Membership<?> membership);

    /**
     * Returns the list of plain attributes for this instance and the given schema name (including membeship attributes,
     * as opposite to {@link Any#getPlainAttr(java.lang.String)}).
     *
     * @param plainSchema plain schema name
     * @return list of plain attributes for this instance and the given schema name (including membeship attributes)
     */
    Collection<PlainAttr> getPlainAttrs(String plainSchema);

    /**
     * Returns the list of plain attributes for this instance and the given membership.
     *
     * @param membership membership
     * @return list of plain attributes for this instance and the given membership
     */
    Collection<PlainAttr> getPlainAttrs(Membership<?> membership);

    boolean add(M membership);

    boolean remove(M membership);

    Optional<? extends M> getMembership(String groupKey);

    List<? extends M> getMemberships();
}
