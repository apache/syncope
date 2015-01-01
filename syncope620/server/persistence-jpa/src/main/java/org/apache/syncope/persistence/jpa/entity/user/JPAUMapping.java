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
package org.apache.syncope.persistence.jpa.entity.user;

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
import org.apache.syncope.persistence.api.entity.MappingItem;
import org.apache.syncope.persistence.api.entity.user.UMapping;
import org.apache.syncope.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.persistence.jpa.entity.AbstractMapping;
import org.apache.syncope.persistence.jpa.entity.JPAExternalResource;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

@Entity
@Table(name = JPAUMapping.TABLE)
public class JPAUMapping extends AbstractMapping<UMappingItem> implements UMapping {

    private static final long serialVersionUID = 4285801404504561073L;

    public static final String TABLE = "UMapping";

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
    private List<JPAUMappingItem> items;

    public JPAUMapping() {
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
    public void setAccountIdItem(final UMappingItem item) {
        checkType(item, JPAUMappingItem.class);
        this.addAccountIdItem((JPAUMappingItem) item);
    }

    @Override
    public UMappingItem getPasswordItem() {
        UMappingItem passwordItem = null;
        for (MappingItem item : getItems()) {
            if (item.isPassword()) {
                passwordItem = (JPAUMappingItem) item;
            }
        }
        return passwordItem;
    }

    @Override
    public boolean setPasswordItem(final UMappingItem passwordItem) {
        checkType(passwordItem, JPAUMappingItem.class);

        passwordItem.setExtAttrName(OperationalAttributes.PASSWORD_NAME);
        passwordItem.setPassword(true);
        return this.addItem((JPAUMappingItem) passwordItem);
    }

    @Override
    public List<? extends UMappingItem> getItems() {
        return items;
    }

    @Override
    public boolean addItem(final UMappingItem item) {
        checkType(item, JPAUMappingItem.class);
        return items.contains((JPAUMappingItem) item) || items.add((JPAUMappingItem) item);
    }

    @Override
    public boolean removeItem(final UMappingItem item) {
        checkType(item, JPAUMappingItem.class);
        return items.remove((JPAUMappingItem) item);
    }
}
