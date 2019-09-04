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
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;

@Entity
@Table(name = JPAOrgUnit.TABLE)
@Cacheable
public class JPAOrgUnit extends AbstractGeneratedKeyEntity implements OrgUnit {

    private static final long serialVersionUID = 8236319635989067603L;

    public static final String TABLE = "OrgUnit";

    @OneToOne
    private JPAExternalResource resource;

    @NotNull
    private String objectClass;

    @Lob
    private String serializedSyncToken;

    @NotNull
    private Boolean ignoreCaseMatch = false;

    @NotNull
    private String connObjectLink;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "orgUnit")
    private List<JPAOrgUnitItem> items = new ArrayList<>();

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
    public ObjectClass getObjectClass() {
        return Optional.ofNullable(objectClass).map(ObjectClass::new).orElse(null);
    }

    @Override
    public void setObjectClass(final ObjectClass objectClass) {
        this.objectClass = Optional.ofNullable(objectClass).map(ObjectClass::getObjectClassValue).orElse(null);
    }

    @Override
    public SyncToken getSyncToken() {
        return Optional.ofNullable(serializedSyncToken)
            .map(syncToken -> POJOHelper.deserialize(syncToken, SyncToken.class)).orElse(null);
    }

    @Override
    public String getSerializedSyncToken() {
        return this.serializedSyncToken;
    }

    @Override
    public void setSyncToken(final SyncToken syncToken) {
        this.serializedSyncToken = Optional.ofNullable(syncToken).map(POJOHelper::serialize).orElse(null);
    }

    @Override
    public boolean isIgnoreCaseMatch() {
        return ignoreCaseMatch;
    }

    @Override
    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
    }

    @Override
    public String getConnObjectLink() {
        return connObjectLink;
    }

    @Override
    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    @Override
    public boolean add(final OrgUnitItem item) {
        checkType(item, JPAOrgUnitItem.class);
        return items.contains((JPAOrgUnitItem) item) || items.add((JPAOrgUnitItem) item);
    }

    @Override
    public List<? extends OrgUnitItem> getItems() {
        return items;
    }

    @Override
    public Optional<? extends OrgUnitItem> getConnObjectKeyItem() {
        return getItems().stream().filter(OrgUnitItem::isConnObjectKey).findFirst();
    }

    @Override
    public void setConnObjectKeyItem(final OrgUnitItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }
}
