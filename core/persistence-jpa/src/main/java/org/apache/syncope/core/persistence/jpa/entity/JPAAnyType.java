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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.common.validation.AnyTypeCheck;

@Entity
@Table(name = JPAAnyType.TABLE)
@AnyTypeCheck
@Cacheable
public class JPAAnyType extends AbstractProvidedKeyEntity implements AnyType {

    private static final long serialVersionUID = 2668267884059219835L;

    public static final String TABLE = "AnyType";

    @NotNull
    @Enumerated(EnumType.STRING)
    private AnyTypeKind kind;

    @ManyToMany
    @JoinTable(joinColumns =
            @JoinColumn(name = "anyType_id", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id", referencedColumnName = "id"))
    private Set<JPAAnyTypeClass> classes = new HashSet<>();

    @Override
    public AnyTypeKind getKind() {
        return kind;
    }

    @Override
    public void setKind(final AnyTypeKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean add(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, JPAAnyTypeClass.class);
        return classes.add((JPAAnyTypeClass) anyTypeClass);
    }

    @Override
    public Set<? extends AnyTypeClass> getClasses() {
        return classes;
    }
}
