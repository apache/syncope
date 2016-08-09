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
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;

@Entity
@Table(name = JPAProvision.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "resource_id", "anyType_id" }))
public class JPAProvision extends AbstractGeneratedKeyEntity implements Provision {

    private static final long serialVersionUID = -1807889487945989443L;

    public static final String TABLE = "Provision";

    @ManyToOne
    private JPAExternalResource resource;

    @ManyToOne
    private JPAAnyType anyType;

    @NotNull
    private String objectClass;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "provision_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @Lob
    private String serializedSyncToken;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "provision")
    private JPAMapping mapping;

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
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public ObjectClass getObjectClass() {
        return objectClass == null
                ? null
                : new ObjectClass(objectClass);
    }

    @Override
    public void setObjectClass(final ObjectClass objectClass) {
        this.objectClass = objectClass == null ? null : objectClass.getObjectClassValue();
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return auxClasses.contains((JPAAnyTypeClass) auxClass) || auxClasses.add((JPAAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public SyncToken getSyncToken() {
        return serializedSyncToken == null
                ? null
                : POJOHelper.deserialize(serializedSyncToken, SyncToken.class);
    }

    @Override
    public String getSerializedSyncToken() {
        return this.serializedSyncToken;
    }

    @Override
    public void setSyncToken(final SyncToken syncToken) {
        this.serializedSyncToken = syncToken == null ? null : POJOHelper.serialize(syncToken);
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(final Mapping mapping) {
        checkType(mapping, JPAMapping.class);
        this.mapping = (JPAMapping) mapping;
    }
}
