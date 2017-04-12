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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMappingItem;
import org.apache.syncope.core.persistence.jpa.validation.entity.SAML2IdPCheck;

@Entity
@Table(name = JPASAML2IdP.TABLE)
@Cacheable
@SAML2IdPCheck
public class JPASAML2IdP extends AbstractGeneratedKeyEntity implements SAML2IdP {

    private static final long serialVersionUID = -392372595500355552L;

    public static final String TABLE = "SAML2IdP";

    @Column(unique = true, nullable = false)
    private String entityID;

    @Column(unique = true, nullable = false)
    private String name;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private Byte[] metadata;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<JPAMappingItem> mappingItems = new ArrayList<>();

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer useDeflateEncoding;

    @Column(nullable = false)
    private SAML2BindingType bindingType;

    @Override
    public String getEntityID() {
        return entityID;
    }

    @Override
    public void setEntityID(final String entityID) {
        this.entityID = entityID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public byte[] getMetadata() {
        return metadata == null ? null : ArrayUtils.toPrimitive(metadata);
    }

    @Override
    public void setMetadata(final byte[] metadata) {
        this.metadata = metadata == null ? null : ArrayUtils.toObject(metadata);
    }

    @Override
    public boolean isUseDeflateEncoding() {
        return isBooleanAsInteger(useDeflateEncoding);
    }

    @Override
    public void setUseDeflateEncoding(final boolean useDeflateEncoding) {
        this.useDeflateEncoding = getBooleanAsInteger(useDeflateEncoding);
    }

    @Override
    public SAML2BindingType getBindingType() {
        return bindingType;
    }

    @Override
    public void setBindingType(final SAML2BindingType bindingType) {
        this.bindingType = bindingType;
    }

    @Override
    public boolean add(final MappingItem item) {
        checkType(item, JPAMappingItem.class);
        return mappingItems.contains((JPAMappingItem) item) || mappingItems.add((JPAMappingItem) item);
    }

    @Override
    public List<? extends MappingItem> getMappingItems() {
        return mappingItems;
    }

    @Override
    public MappingItem getConnObjectKeyItem() {
        return IterableUtils.find(getMappingItems(), new Predicate<MappingItem>() {

            @Override
            public boolean evaluate(final MappingItem item) {
                return item.isConnObjectKey();
            }
        });
    }

    @Override
    public void setConnObjectKeyItem(final MappingItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }

}
