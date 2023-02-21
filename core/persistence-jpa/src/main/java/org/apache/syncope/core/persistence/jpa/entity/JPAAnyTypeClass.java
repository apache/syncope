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

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

@Entity
@Table(name = JPAAnyTypeClass.TABLE)
@Cacheable
public class JPAAnyTypeClass extends AbstractProvidedKeyEntity implements AnyTypeClass {

    private static final long serialVersionUID = -1750247153774475453L;

    public static final String TABLE = "AnyTypeClass";

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "anyTypeClass")
    private List<JPAPlainSchema> plainSchemas = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "anyTypeClass")
    private List<JPADerSchema> derSchemas = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "anyTypeClass")
    private List<JPAVirSchema> virSchemas = new ArrayList<>();

    @Override
    public boolean add(final PlainSchema schema) {
        checkType(schema, JPAPlainSchema.class);
        return this.plainSchemas.add((JPAPlainSchema) schema);
    }

    @Override
    public List<? extends PlainSchema> getPlainSchemas() {
        return plainSchemas;
    }

    @Override
    public boolean add(final DerSchema schema) {
        checkType(schema, JPADerSchema.class);
        return this.derSchemas.add((JPADerSchema) schema);
    }

    @Override
    public List<? extends DerSchema> getDerSchemas() {
        return derSchemas;
    }

    @Override
    public boolean add(final VirSchema schema) {
        checkType(schema, JPAVirSchema.class);
        return this.virSchemas.add((JPAVirSchema) schema);
    }

    @Override
    public List<? extends VirSchema> getVirSchemas() {
        return virSchemas;
    }
}
