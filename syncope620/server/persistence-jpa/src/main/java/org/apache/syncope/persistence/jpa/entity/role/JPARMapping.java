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
package org.apache.syncope.persistence.jpa.entity.role;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.role.RMapping;
import org.apache.syncope.persistence.api.entity.role.RMappingItem;
import org.apache.syncope.persistence.jpa.entity.AbstractMapping;
import org.apache.syncope.persistence.jpa.entity.JPAExternalResource;

@Entity
@Table(name = JPARMapping.TABLE)
public class JPARMapping extends AbstractMapping<RMappingItem> implements RMapping {

    public static final String TABLE = "RMapping";

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
    private List<JPARMappingItem> items;

    public JPARMapping() {
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
    public void setAccountIdItem(final RMappingItem item) {
        checkType(item, JPARMappingItem.class);
        this.addAccountIdItem((JPARMappingItem) item);
    }

    @Override
    public List<? extends RMappingItem> getItems() {
        return items;
    }

    @Override
    public boolean addItem(final RMappingItem item) {
        checkType(item, JPARMappingItem.class);
        return items.contains((JPARMappingItem) item) || items.add((JPARMappingItem) item);
    }

    @Override
    public boolean removeItem(final RMappingItem item) {
        checkType(item, JPARMappingItem.class);
        return items.remove((JPARMappingItem) item);
    }
}
