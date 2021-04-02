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

import javax.persistence.Basic;
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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
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
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "provision_id", "anyTypeClass_id" }))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @Lob
    private String serializedSyncToken;

    @Basic
    private Boolean ignoreCaseMatch = false;

    @ManyToOne
    private JPAPlainSchema uidOnCreate;

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
        return Optional.ofNullable(objectClass).map(ObjectClass::new).orElse(null);
    }

    @Override
    public void setObjectClass(final ObjectClass objectClass) {
        this.objectClass = Optional.ofNullable(objectClass).map(ObjectClass::getObjectClassValue).orElse(null);
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
        return BooleanUtils.isNotFalse(ignoreCaseMatch);
    }

    @Override
    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
    }

    @Override
    public PlainSchema getUidOnCreate() {
        return uidOnCreate;
    }

    @Override
    public void setUidOnCreate(final PlainSchema uidOnCreate) {
        checkType(uidOnCreate, JPAPlainSchema.class);
        this.uidOnCreate = (JPAPlainSchema) uidOnCreate;
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
