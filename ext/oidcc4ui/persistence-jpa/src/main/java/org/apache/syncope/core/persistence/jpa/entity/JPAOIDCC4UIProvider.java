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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.OIDCClientImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;
import org.apache.syncope.core.persistence.api.validation.OIDCC4UIProviderCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.util.CollectionUtils;

@Entity
@Table(name = JPAOIDCC4UIProvider.TABLE)
@Cacheable
@OIDCC4UIProviderCheck
public class JPAOIDCC4UIProvider extends AbstractGeneratedKeyEntity implements OIDCC4UIProvider {

    public static final String TABLE = "OIDCProvider";

    private static final long serialVersionUID = 1423093003585826403L;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String clientID;

    @Column(unique = true, nullable = false)
    private String clientSecret;

    @Column(nullable = false)
    private String authorizationEndpoint;

    @Column(nullable = false)
    private String tokenEndpoint;

    @Column(nullable = false)
    private String jwksUri;

    @Column(nullable = false)
    private String issuer;

    @Column(nullable = true)
    private String userinfoEndpoint;

    @Column(nullable = true)
    private String endSessionEndpoint;

    private String scopes;

    @Column(nullable = false)
    private boolean hasDiscovery;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "op")
    private JPAOIDCC4UIUserTemplate userTemplate;

    @Lob
    private String items;

    @Transient
    private final List<Item> itemList = new ArrayList<>();

    @NotNull
    private Boolean createUnmatching = false;

    @NotNull
    private Boolean selfRegUnmatching = false;

    @NotNull
    private Boolean updateMatching = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "OIDCProviderAction",
            joinColumns =
            @JoinColumn(name = "op_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> actions = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getClientID() {
        return clientID;
    }

    @Override
    public void setClientID(final String clientID) {
        this.clientID = clientID;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    @Override
    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    @Override
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    @Override
    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String getJwksUri() {
        return jwksUri;
    }

    @Override
    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    @Override
    public void setUserinfoEndpoint(final String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    @Override
    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    @Override
    public void setEndSessionEndpoint(final String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    @Override
    public List<String> getScopes() {
        return Optional.ofNullable(scopes).map(s -> Stream.of(s.split(" ")).toList()).orElseGet(List::of);
    }

    @Override
    public void setScopes(final List<String> scopes) {
        this.scopes = CollectionUtils.isEmpty(scopes)
                ? ""
                : String.join(" ", scopes);
    }

    @Override
    public boolean getHasDiscovery() {
        return hasDiscovery;
    }

    @Override
    public void setHasDiscovery(final boolean hasDiscovery) {
        this.hasDiscovery = hasDiscovery;
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
    public OIDCC4UIUserTemplate getUserTemplate() {
        return userTemplate;
    }

    @Override
    public void setUserTemplate(final OIDCC4UIUserTemplate userTemplate) {
        checkType(userTemplate, JPAOIDCC4UIUserTemplate.class);
        this.userTemplate = (JPAOIDCC4UIUserTemplate) userTemplate;
    }

    @Override
    public List<Item> getItems() {
        return itemList;
    }

    @Override
    public Optional<Item> getConnObjectKeyItem() {
        return getItems().stream().filter(Item::isConnObjectKey).findFirst();
    }

    @Override
    public void setConnObjectKeyItem(final Item item) {
        item.setConnObjectKey(true);
        getItems().add(item);
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, OIDCClientImplementationType.OP_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getItems().clear();
        }
        if (items != null) {
            getItems().addAll(
                    POJOHelper.deserialize(items, new TypeReference<List<Item>>() {
                    }));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        items = POJOHelper.serialize(getItems());
    }
}
