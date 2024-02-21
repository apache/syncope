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
package org.apache.syncope.core.persistence.jpa.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.common.validation.SchemaKeyCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = AbstractSchema.TABLE)
@Cacheable
@SchemaKeyCheck
public abstract class AbstractSchema extends AbstractProvidedKeyEntity implements Schema {

    private static final long serialVersionUID = -9222344997225831269L;

    public static final String TABLE = "SyncopeSchema";

    protected static final TypeReference<HashMap<Locale, String>> LABEL_TYPEREF =
            new TypeReference<HashMap<Locale, String>>() {
    };

    @Lob
    private String labels;

    @Transient
    private Map<Locale, String> labelMap = new HashMap<>();

    @Override
    public Optional<String> getLabel(final Locale locale) {
        return Optional.ofNullable(labelMap.get(locale));
    }

    @Override
    public Map<Locale, String> getLabels() {
        return labelMap;
    }

    protected void json2map(final boolean clearFirst) {
        if (clearFirst) {
            getLabels().clear();
        }
        if (labels != null) {
            getLabels().putAll(POJOHelper.deserialize(labels, LABEL_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2map(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2map(true);
    }

    @PrePersist
    @PreUpdate
    public void map2json() {
        labels = POJOHelper.serialize(getLabels());
    }
}
