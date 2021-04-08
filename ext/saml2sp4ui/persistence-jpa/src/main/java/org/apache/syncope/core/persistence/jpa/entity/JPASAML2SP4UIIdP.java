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
import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SP4UIImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdPItem;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.jpa.validation.entity.SAML2SP4UIIdPCheck;

@Entity
@Table(name = JPASAML2SP4UIIdP.TABLE)
@Cacheable
@SAML2SP4UIIdPCheck
public class JPASAML2SP4UIIdP extends AbstractGeneratedKeyEntity implements SAML2SP4UIIdP {

    private static final long serialVersionUID = -392372595500355552L;

    public static final String TABLE = "SAML2SP4UIIdP";

    @Column(unique = true, nullable = false)
    private String entityID;

    @Column(unique = true, nullable = false)
    private String name;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private Byte[] metadata;

    @NotNull
    private Boolean logoutSupported = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "idp")
    private List<JPASAML2SP4UIIdPItem> items = new ArrayList<>();

    @NotNull
    private Boolean createUnmatching = false;

    @NotNull
    private Boolean selfRegUnmatching = false;

    @NotNull
    private Boolean updateMatching = false;

    @Column(nullable = false)
    private SAML2BindingType bindingType;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "idp")
    private JPASAML2SP4UIUserTemplate userTemplate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "SAML2IdP4UIAction",
            joinColumns =
            @JoinColumn(name = "saml2idp4ui_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToOne
    private JPAImplementation requestedAuthnContextProvider;

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
        return Optional.ofNullable(metadata).map(ArrayUtils::toPrimitive).orElse(null);
    }

    @Override
    public void setMetadata(final byte[] metadata) {
        this.metadata = Optional.ofNullable(metadata).map(ArrayUtils::toObject).orElse(null);
    }

    @Override
    public boolean isLogoutSupported() {
        return logoutSupported;
    }

    @Override
    public void setLogoutSupported(final boolean logoutSupported) {
        this.logoutSupported = logoutSupported;
    }

    @Override
    public boolean isCreateUnmatching() {
        return createUnmatching;
    }

    @Override
    public void setCreateUnmatching(final boolean createUnmatching) {
        this.createUnmatching = createUnmatching;
    }

    @Override
    public boolean isSelfRegUnmatching() {
        return selfRegUnmatching;
    }

    @Override
    public void setSelfRegUnmatching(final boolean selfRegUnmatching) {
        this.selfRegUnmatching = selfRegUnmatching;
    }

    @Override
    public boolean isUpdateMatching() {
        return updateMatching;
    }

    @Override
    public void setUpdateMatching(final boolean updateMatching) {
        this.updateMatching = updateMatching;
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
    public SAML2SP4UIUserTemplate getUserTemplate() {
        return userTemplate;
    }

    @Override
    public void setUserTemplate(final SAML2SP4UIUserTemplate userTemplate) {
        checkType(userTemplate, JPASAML2SP4UIUserTemplate.class);
        this.userTemplate = (JPASAML2SP4UIUserTemplate) userTemplate;
    }

    @Override
    public boolean add(final SAML2SP4UIIdPItem item) {
        checkType(item, JPASAML2SP4UIIdPItem.class);
        return items.contains((JPASAML2SP4UIIdPItem) item) || items.add((JPASAML2SP4UIIdPItem) item);
    }

    @Override
    public List<? extends SAML2SP4UIIdPItem> getItems() {
        return items;
    }

    @Override
    public Optional<? extends SAML2SP4UIIdPItem> getConnObjectKeyItem() {
        return getItems().stream().filter(Item::isConnObjectKey).findFirst();
    }

    @Override
    public void setConnObjectKeyItem(final SAML2SP4UIIdPItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, SAML2SP4UIImplementationType.IDP_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    @Override
    public JPAImplementation getRequestedAuthnContextProvider() {
        return requestedAuthnContextProvider;
    }

    @Override
    public void setRequestedAuthnContextProvider(final Implementation requestedAuthnContextProvider) {
        checkType(requestedAuthnContextProvider, JPAImplementation.class);
        checkImplementationType(requestedAuthnContextProvider,
                SAML2SP4UIImplementationType.REQUESTED_AUTHN_CONTEXT_PROVIDER);
        this.requestedAuthnContextProvider = (JPAImplementation) requestedAuthnContextProvider;
    }
}
