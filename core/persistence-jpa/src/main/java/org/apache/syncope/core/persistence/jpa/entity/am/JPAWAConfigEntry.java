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
package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAWAConfigEntry.TABLE)
public class JPAWAConfigEntry extends AbstractProvidedKeyEntity implements WAConfigEntry {

    private static final long serialVersionUID = 6422422526695279794L;

    public static final String TABLE = "WAConfigEntry";

    protected static TypeReference<List<String>> TYPEREF = new TypeReference<List<String>>() {
    };

    @Lob
    private String waConfigValues;

    @Override
    public List<String> getValues() {
        return Optional.ofNullable(waConfigValues).map(v -> POJOHelper.deserialize(v, TYPEREF)).orElseGet(List::of);
    }

    @Override
    public void setValues(final List<String> values) {
        this.waConfigValues = POJOHelper.serialize(values);
    }
}
