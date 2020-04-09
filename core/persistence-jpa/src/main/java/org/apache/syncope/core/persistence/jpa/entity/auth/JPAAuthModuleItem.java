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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModuleItem;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.resource.AbstractItem;

@Entity
@Table(name = JPAAuthModuleItem.TABLE)
@Cacheable
public class JPAAuthModuleItem extends AbstractItem implements AuthModuleItem {

    public static final String TABLE = "AuthModuleItem";

    private static final long serialVersionUID = 3165440920144995781L;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Transformer",
            joinColumns =
            @JoinColumn(name = "item_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "item_id", "implementation_id" }))
    private final List<JPAImplementation> transformers = new ArrayList<>();

    @Override
    public boolean add(final Implementation transformer) {
        checkType(transformer, JPAImplementation.class);
        checkImplementationType(transformer, IdMImplementationType.ITEM_TRANSFORMER);
        return transformers.contains((JPAImplementation) transformer)
                || this.transformers.add((JPAImplementation) transformer);
    }

    @Override
    public List<? extends Implementation> getTransformers() {
        return transformers;
    }

}
