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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAWAConfigEntry.TABLE)
public class JPAWAConfigEntry extends AbstractProvidedKeyEntity implements WAConfigEntry {

    public static final String TABLE = "WAConfigEntry";

    private static final long serialVersionUID = 6422422526695279794L;

    @Lob
    private String waConfigValues;

    @Override
    public List<String> getValues() {
        return waConfigValues == null
                ? List.of()
                : POJOHelper.deserialize(waConfigValues, new TypeReference<>() {
        });
    }

    @Override
    public void setValues(final List<String> values) {
        this.waConfigValues = POJOHelper.serialize(values);
    }
}
