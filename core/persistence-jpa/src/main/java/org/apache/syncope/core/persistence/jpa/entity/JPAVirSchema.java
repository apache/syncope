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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.LinkingMappingItem;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;

@Entity
@Table(name = JPAVirSchema.TABLE)
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class JPAVirSchema extends AbstractSchema implements VirSchema {

    private static final long serialVersionUID = 3274006935328590141L;

    public static final String TABLE = "VirSchema";

    @OneToOne(fetch = FetchType.EAGER)
    private JPAAnyTypeClass anyTypeClass;

    private Boolean readonly = false;

    @NotNull
    @ManyToOne
    private JPAProvision provision;

    @NotNull
    private String extAttrName;

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, JPAAnyTypeClass.class);
        this.anyTypeClass = (JPAAnyTypeClass) anyTypeClass;
    }

    @Override
    public AttrSchemaType getType() {
        return AttrSchemaType.String;
    }

    @Override
    public String getMandatoryCondition() {
        return Boolean.FALSE.toString().toLowerCase();
    }

    @Override
    public boolean isMultivalue() {
        return true;
    }

    @Override
    public boolean isUniqueConstraint() {
        return false;
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

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
    public String getExtAttrName() {
        return extAttrName;
    }

    @Override
    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    @Override
    public MappingItem asLinkingMappingItem() {
        return new LinkingMappingItem(this);
    }

}
