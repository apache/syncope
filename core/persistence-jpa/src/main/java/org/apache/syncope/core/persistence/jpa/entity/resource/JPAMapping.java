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
package org.apache.syncope.core.persistence.jpa.entity.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAMapping.TABLE)
@Cacheable
public class JPAMapping extends AbstractGeneratedKeyEntity implements Mapping {

    private static final long serialVersionUID = 4316047254916259158L;

    public static final String TABLE = "Mapping";

    @OneToOne(optional = false)
    private JPAProvision provision;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "mapping")
    private List<JPAMappingItem> items = new ArrayList<>();

    /**
     * A JEXL expression for determining how to find the connector object link in external resource's space.
     */
    private String connObjectLink;

    @Override
    public Provision getProvision() {
        return provision;
    }

    @Override
    public void setProvision(final Provision provision) {
        checkType(provision, JPAProvision.class);
        this.provision = (JPAProvision) provision;
    }

    @Override
    public boolean add(final MappingItem item) {
        checkType(item, JPAMappingItem.class);
        return items.contains((JPAMappingItem) item) || items.add((JPAMappingItem) item);
    }

    @Override
    public List<? extends MappingItem> getItems() {
        return items;
    }

    @Override
    public Optional<? extends MappingItem> getConnObjectKeyItem() {
        return getItems().stream().filter(MappingItem::isConnObjectKey).findFirst();
    }

    @Override
    public void setConnObjectKeyItem(final MappingItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }

    @Override
    public String getConnObjectLink() {
        return connObjectLink;
    }

    @Override
    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }
}
