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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.SAML2IdPItem;
import org.apache.syncope.core.persistence.api.entity.SAML2UserTemplate;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "idp")
    private List<JPASAML2IdPItem> items = new ArrayList<>();

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer createUnmatching;

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer updateMatching;

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer useDeflateEncoding;

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer supportUnsolicited;

    @Column(nullable = false)
    private SAML2BindingType bindingType;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "idp")
    private JPASAML2UserTemplate userTemplate;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = TABLE + "_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "saml2IdP_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

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
    public boolean isCreateUnmatching() {
        return isBooleanAsInteger(createUnmatching);
    }

    @Override
    public void setCreateUnmatching(final boolean createUnmatching) {
        this.createUnmatching = getBooleanAsInteger(createUnmatching);
    }

    @Override
    public boolean isUpdateMatching() {
        return isBooleanAsInteger(updateMatching);
    }

    @Override
    public void setUpdateMatching(final boolean updateMatching) {
        this.updateMatching = getBooleanAsInteger(updateMatching);
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
    public boolean isSupportUnsolicited() {
        return isBooleanAsInteger(supportUnsolicited);
    }

    @Override
    public void setSupportUnsolicited(final boolean supportUnsolicited) {
        this.supportUnsolicited = getBooleanAsInteger(supportUnsolicited);
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
    public SAML2UserTemplate getUserTemplate() {
        return userTemplate;
    }

    @Override
    public void setUserTemplate(final SAML2UserTemplate userTemplate) {
        checkType(userTemplate, JPASAML2UserTemplate.class);
        this.userTemplate = (JPASAML2UserTemplate) userTemplate;
    }

    @Override
    public boolean add(final SAML2IdPItem item) {
        checkType(item, JPASAML2IdPItem.class);
        return items.contains((JPASAML2IdPItem) item) || items.add((JPASAML2IdPItem) item);
    }

    @Override
    public List<? extends SAML2IdPItem> getItems() {
        return items;
    }

    @Override
    public Optional<? extends SAML2IdPItem> getConnObjectKeyItem() {
        return getItems().stream().filter(item -> item.isConnObjectKey()).findFirst();
    }

    @Override
    public void setConnObjectKeyItem(final SAML2IdPItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }

    @Override
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
    }
}
