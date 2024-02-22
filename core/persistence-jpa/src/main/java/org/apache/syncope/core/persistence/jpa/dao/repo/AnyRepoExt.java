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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;

public interface AnyRepoExt<A extends Any<?>> {

    Optional<OffsetDateTime> findLastChange(String key);

    A authFind(String key);

    List<A> findByPlainAttrValue(PlainSchema schema, PlainAttrValue attrValue, boolean ignoreCaseMatch);

    Optional<A> findByPlainAttrUniqueValue(
            PlainSchema schema, PlainAttrUniqueValue attrUniqueValue, boolean ignoreCaseMatch);

    /**
     * Find any objects by derived attribute value. This method could fail if one or more string literals contained
     * into the derived attribute value provided derive from identifier (schema key) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string literals used to build the expression
     * cannot be found into the attribute values used to replace attribute schema keys used as identifiers.
     *
     * @param schema derived schema
     * @param value derived attribute value
     * @param ignoreCaseMatch whether comparison for string values should take case into account or not
     * @return list of any objects
     */
    List<A> findByDerAttrValue(DerSchema schema, String value, boolean ignoreCaseMatch);

    <S extends Schema> AllowedSchemas<S> findAllowedSchemas(A any, Class<S> reference);

    List<String> findDynRealms(String key);

    Collection<String> findAllResourceKeys(String key);

    <S extends A> S save(S any);

    void deleteById(String key);

    void delete(A any);
}
