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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.SchemaLabel;
import org.apache.syncope.core.persistence.jpa.validation.entity.SchemaKeyCheck;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = AbstractSchema.TABLE)
@Cacheable
@SchemaKeyCheck
public abstract class AbstractSchema extends AbstractProvidedKeyEntity implements Schema {

    public static final String TABLE = "SyncopeSchema";

    private static final long serialVersionUID = -9222344997225831269L;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "schema")
    private List<JPASchemaLabel> labels = new ArrayList<>();

    @Override
    public boolean add(final SchemaLabel label) {
        checkType(label, JPASchemaLabel.class);
        return this.labels.add((JPASchemaLabel) label);
    }

    @Override
    public Optional<? extends SchemaLabel> getLabel(final Locale locale) {
        return labels.stream().filter(label -> label.getLocale().equals(locale)).findFirst();
    }

    @Override
    public List<? extends SchemaLabel> getLabels() {
        return labels;
    }
}
