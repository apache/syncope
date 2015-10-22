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

import java.util.Collections;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.persistence.jpa.validation.entity.SchemaNameCheck;

@Entity
@Table(name = JPAVirSchema.TABLE)
@Cacheable
@SchemaNameCheck
public class JPAVirSchema extends AbstractEntity<String> implements VirSchema {

    private static final long serialVersionUID = 3274006935328590141L;

    public static final String TABLE = "VirSchema";

    @Id
    private String name;

    @OneToOne(fetch = FetchType.EAGER)
    private JPAAnyTypeClass anyTypeClass;

    @Basic
    @Min(0)
    @Max(1)
    private Integer readonly;

    @Column(nullable = false)
    @ManyToOne
    private JPAProvision provision;

    @Column(nullable = false)
    private String extAttrName;

    public JPAVirSchema() {
        super();

        readonly = getBooleanAsInteger(false);
    }

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public void setKey(final String key) {
        this.name = key;
    }

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
        return Boolean.TRUE;
    }

    @Override
    public boolean isUniqueConstraint() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isReadonly() {
        return isBooleanAsInteger(readonly);
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.readonly = getBooleanAsInteger(readonly);
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
        return new MappingItem() {

            private static final long serialVersionUID = 327455459536715529L;

            @Override
            public Long getKey() {
                return -1L;
            }

            @Override
            public Mapping getMapping() {
                return getProvision().getMapping();
            }

            @Override
            public void setMapping(final Mapping mapping) {
                // RO instance, nothing to do
            }

            @Override
            public String getExtAttrName() {
                return JPAVirSchema.this.getExtAttrName();
            }

            @Override
            public void setExtAttrName(final String extAttrName) {
                // RO instance, nothing to do
            }

            @Override
            public String getIntAttrName() {
                return JPAVirSchema.this.getKey();
            }

            @Override
            public void setIntAttrName(final String intAttrName) {
                // RO instance, nothing to do
            }

            @Override
            public IntMappingType getIntMappingType() {
                switch (getProvision().getAnyType().getKind()) {
                    case ANY_OBJECT:
                        return IntMappingType.AnyObjectVirtualSchema;

                    case GROUP:
                        return IntMappingType.GroupVirtualSchema;

                    case USER:
                    default:
                        return IntMappingType.UserVirtualSchema;
                }
            }

            @Override
            public void setIntMappingType(final IntMappingType intMappingType) {
                // RO instance, nothing to do
            }

            @Override
            public String getMandatoryCondition() {
                return JPAVirSchema.this.getMandatoryCondition();
            }

            @Override
            public void setMandatoryCondition(final String condition) {
                // RO instance, nothing to do
            }

            @Override
            public MappingPurpose getPurpose() {
                return JPAVirSchema.this.isReadonly() ? MappingPurpose.SYNCHRONIZATION : MappingPurpose.BOTH;
            }

            @Override
            public void setPurpose(final MappingPurpose purpose) {
                // RO instance, nothing to do
            }

            @Override
            public boolean isConnObjectKey() {
                return false;
            }

            @Override
            public void setConnObjectKey(final boolean connObjectKey) {
                // RO instance, nothing to do
            }

            @Override
            public boolean isPassword() {
                return false;
            }

            @Override
            public void setPassword(final boolean password) {
                // RO instance, nothing to do
            }

            @Override
            public List<String> getMappingItemTransformerClassNames() {
                return Collections.emptyList();
            }
        };
    }

}
