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
package org.apache.syncope.core.persistence.neo4j.converters;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import tools.jackson.core.type.TypeReference;

abstract class SerializableSetConverter<T extends Serializable> implements Neo4jPersistentPropertyConverter<Set<T>> {

    protected abstract TypeReference<Set<T>> typeRef();

    @Override
    public Value write(final Set<T> source) {
        return Optional.ofNullable(source).map(POJOHelper::serialize).map(Values::value).
                orElse(Values.value(Set.of()));
    }

    @Override
    public Set<T> read(final Value source) {
        return Optional.ofNullable(source).
                map(data -> POJOHelper.deserialize(source.asString(), typeRef())).orElseGet(HashSet::new);
    }
}
