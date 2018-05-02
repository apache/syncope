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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "oidcprovider")
@XmlType
public class OIDCProviderTO extends AbstractBaseBean implements EntityTO, ItemContainerTO {

    private static final long serialVersionUID = -1229802774546135794L;

    private String key;

    private String name;

    private String clientID;

    private String clientSecret;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String jwksUri;

    private String issuer;

    private String userinfoEndpoint;

    private boolean hasDiscovery;

    private UserTO userTemplate;

    private boolean createUnmatching;

    private boolean updateMatching;

    private final List<ItemTO> items = new ArrayList<>();

    private final Set<String> actionsClassNames = new HashSet<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(final String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(final String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public UserTO getUserTemplate() {
        return userTemplate;
    }

    public boolean getHasDiscovery() {
        return hasDiscovery;
    }

    public void setHasDiscovery(final boolean hasDiscovery) {
        this.hasDiscovery = hasDiscovery;
    }

    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = userTemplate;
    }

    public boolean isCreateUnmatching() {
        return createUnmatching;
    }

    public void setCreateUnmatching(final boolean createUnmatching) {
        this.createUnmatching = createUnmatching;
    }

    public boolean isUpdateMatching() {
        return updateMatching;
    }

    public void setUpdateMatching(final boolean updateMatching) {
        this.updateMatching = updateMatching;
    }

    @Override
    public ItemTO getConnObjectKeyItem() {
        return getItems().stream().filter(ItemTO::isConnObjectKey).findFirst().get();
    }

    protected boolean addConnObjectKeyItem(final ItemTO connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return this.add(connObjectItem);
    }

    @Override
    public boolean setConnObjectKeyItem(final ItemTO connObjectKeyItem) {
        return connObjectKeyItem == null
                ? remove(getConnObjectKeyItem())
                : addConnObjectKeyItem(connObjectKeyItem);
    }

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    @JsonProperty("items")
    @Override
    public List<ItemTO> getItems() {
        return items;
    }

    @Override
    public boolean add(final ItemTO item) {
        return item == null ? false : this.items.contains(item) || this.items.add(item);
    }

    public boolean remove(final ItemTO item) {
        return this.items.remove(item);
    }

    @XmlElementWrapper(name = "actionsClassNames")
    @XmlElement(name = "actionsClassName")
    @JsonProperty("actionsClassNames")
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
    }

}
