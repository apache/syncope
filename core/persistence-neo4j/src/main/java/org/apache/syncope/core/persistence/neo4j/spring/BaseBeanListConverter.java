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
package org.apache.syncope.core.persistence.neo4j.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.NullValue;
import org.neo4j.driver.internal.value.StringValue;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import tools.jackson.core.type.TypeReference;

public class BaseBeanListConverter<T extends BaseBean> implements Neo4jPersistentPropertyConverter<List<T>> {

    protected final TypeReference<List<T>> typeRef = new TypeReference<List<T>>() {
    };

    @Override
    public Value write(final List<T> source) {
        return Optional.ofNullable(source).
                flatMap(n -> Optional.ofNullable(POJOHelper.serialize(n))).
                map(n -> (Value) new StringValue(n)).
                orElseGet(() -> NullValue.NULL);
    }

    @Override
    public List<T> read(final Value source) {
        return Optional.ofNullable(source).
                filter(StringValue.class::isInstance).
                map(StringValue.class::cast).
                map(data -> POJOHelper.deserialize(data.asString(), typeRef)).
                orElseGet(ArrayList::new);
    }
}
