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
package org.apache.syncope.core.persistence.jpa.entity.group;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.group.GMapping;
import org.apache.syncope.core.persistence.api.entity.group.GMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.AbstractMapping;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;

@Entity
@Table(name = JPAGMapping.TABLE)
public class JPAGMapping extends AbstractMapping<GMappingItem> implements GMapping {

    public static final String TABLE = "GMapping";

    private static final long serialVersionUID = 4578756002867863392L;

    @Id
    private Long id;

    /**
     * Resource owning this mapping.
     */
    @OneToOne
    private JPAExternalResource resource;

    /**
     * Attribute mappings.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "mapping")
    private List<JPAGMappingItem> items;

    public JPAGMapping() {
        super();

        items = new ArrayList<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        this.resource = (JPAExternalResource) resource;
    }

    @Override
    public void setAccountIdItem(final GMappingItem item) {
        checkType(item, JPAGMappingItem.class);
        this.addAccountIdItem((JPAGMappingItem) item);
    }

    @Override
    public List<? extends GMappingItem> getItems() {
        return items;
    }

    @Override
    public boolean addItem(final GMappingItem item) {
        checkType(item, JPAGMappingItem.class);
        return items.contains((JPAGMappingItem) item) || items.add((JPAGMappingItem) item);
    }

    @Override
    public boolean removeItem(final GMappingItem item) {
        checkType(item, JPAGMappingItem.class);
        return items.remove((JPAGMappingItem) item);
    }
}
