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
package org.apache.syncope.core.persistence.beans.role;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ExternalResource;

@Entity
public class RMapping extends AbstractMapping {

    private static final long serialVersionUID = -4370284858054993282L;

    @Id
    private Long id;

    /**
     * Resource owning this mapping.
     */
    @OneToOne
    private ExternalResource resource;

    /*
     * Attribute mappings.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "mapping")
    private List<RMappingItem> items;

    public RMapping() {
        super();

        items = new ArrayList<RMappingItem>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        this.resource = resource;
    }

    @Override
    public <T extends AbstractMappingItem> void setAccountIdItem(final T accountIdItem) {
        if (accountIdItem != null && !(accountIdItem instanceof RMappingItem)) {
            throw new ClassCastException("accountIdItem is expected to be typed RMappingItem: "
                    + accountIdItem.getClass().getName());
        }

        this.addAccountIdItem(accountIdItem);
    }

    @Override
    public <T extends AbstractMappingItem> List<T> getItems() {
        return (List<T>) this.items;
    }

    @Override
    public <T extends AbstractMappingItem> boolean addItem(final T item) {
        if (item != null && !(item instanceof RMappingItem)) {
            throw new ClassCastException("items are expected to be typed RMappingItem: " + item.getClass().getName());
        }
        return item == null ? false : this.items.contains((RMappingItem) item) || this.items.add((RMappingItem) item);
    }

    @Override
    public <T extends AbstractMappingItem> boolean removeItem(final T item) {
        if (item != null && !(item instanceof RMappingItem)) {
            throw new ClassCastException("items are expected to be typed RMappingItem: " + item.getClass().getName());
        }
        return this.items.remove((RMappingItem) item);
    }

    @Override
    public <T extends AbstractMappingItem> void setItems(final List<T> items) {
        this.items.clear();
        if (items != null && !items.isEmpty()) {
            T item = items.iterator().next();
            if (!(item instanceof RMappingItem)) {
                throw new ClassCastException("items are expected to be typed RMappingItem: "
                        + item.getClass().getName());
            }
            this.items.addAll((List<RMappingItem>) items);
        }
    }
}
