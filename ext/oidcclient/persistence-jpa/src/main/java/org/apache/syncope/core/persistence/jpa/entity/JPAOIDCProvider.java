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
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCProviderItem;
import org.apache.syncope.core.persistence.api.entity.OIDCUserTemplate;

@Entity
@Table(name = JPAOIDCProvider.TABLE)
@Cacheable
public class JPAOIDCProvider extends AbstractGeneratedKeyEntity implements OIDCProvider {

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

    @Column(nullable = false)
    private boolean hasDiscovery;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "op")
    private JPAOIDCUserTemplate userTemplate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "op")
    private List<JPAOIDCProviderItem> items = new ArrayList<>();

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer createUnmatching;

    @Min(0)
    @Max(1)
    @Column(nullable = false)
    private Integer updateMatching;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = TABLE + "_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "oidcOP_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

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
    public boolean getHasDiscovery() {
        return hasDiscovery;
    }

    @Override
    public void setHasDiscovery(final boolean hasDiscovery) {
        this.hasDiscovery = hasDiscovery;
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
    public OIDCUserTemplate getUserTemplate() {
        return userTemplate;
    }

    @Override
    public void setUserTemplate(final OIDCUserTemplate userTemplate) {
        checkType(userTemplate, JPAOIDCUserTemplate.class);
        this.userTemplate = (JPAOIDCUserTemplate) userTemplate;
    }

    @Override
    public boolean add(final OIDCProviderItem item) {
        checkType(item, JPAOIDCProviderItem.class);
        return items.contains((JPAOIDCProviderItem) item) || items.add((JPAOIDCProviderItem) item);
    }

    @Override
    public List<? extends OIDCProviderItem> getItems() {
        return items;
    }

    @Override
    public OIDCProviderItem getConnObjectKeyItem() {
        return IterableUtils.find(getItems(), new Predicate<OIDCProviderItem>() {

            @Override
            public boolean evaluate(final OIDCProviderItem item) {
                return item.isConnObjectKey();
            }
        });
    }

    @Override
    public void setConnObjectKeyItem(final OIDCProviderItem item) {
        item.setConnObjectKey(true);
        this.add(item);
    }

    @Override
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
    }

}
