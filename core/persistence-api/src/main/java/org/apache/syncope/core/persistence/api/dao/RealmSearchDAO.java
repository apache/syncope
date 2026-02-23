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
package org.apache.syncope.core.persistence.api.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.data.domain.Pageable;

public interface RealmSearchDAO {

    Optional<Realm> findByFullPath(String fullPath);

    List<Realm> findByName(String name);

    List<Realm> findChildren(Realm realm);

    List<Realm> findDescendants(String base, String prefix);

    default void findAncestors(final List<Realm> result, final Realm realm) {
        if (realm.getParent() != null && !result.contains(realm.getParent())) {
            result.add(realm.getParent());
            findAncestors(result, realm.getParent());
        }
    }

    default List<Realm> findAncestors(Realm realm) {
        List<Realm> result = new ArrayList<>();
        result.add(realm);
        findAncestors(result, realm);
        return result;
    }

    /**
     * Find realmss by derived attribute value. This method could fail if one or more string literals contained
     * into the derived attribute value provided derive from identifier (schema key) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string literals used to build the expression
     * cannot be found into the attribute values used to replace attribute schema keys used as identifiers.
     *
     * @param expression JEXL expression
     * @param value derived attribute value
     * @param ignoreCaseMatch whether comparison for string values should take case into account or not
     * @return list of realms
     */
    List<Realm> findByDerAttrValue(String expression, String value, boolean ignoreCaseMatch);

    long count(Set<String> bases, SearchCond cond);

    List<Realm> search(Set<String> bases, SearchCond cond, Pageable pageable);

    default SearchCond getAllMatchingCond() {
        AnyCond idCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.of(idCond);
    }
}
