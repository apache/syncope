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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.PathParam;
import org.apache.syncope.common.lib.types.SAML2BindingType;

public class SAML2SP4UIIdPTO implements EntityTO, ItemContainerTO {

    private static final long serialVersionUID = 4426527052873779881L;

    private String key;

    private String entityID;

    private String name;

    private String metadata;

    private boolean createUnmatching;

    private boolean updateMatching;

    private boolean selfRegUnmatching;

    private SAML2BindingType bindingType;

    private boolean logoutSupported;

    private UserTO userTemplate;

    private final List<ItemTO> items = new ArrayList<>();

    private final List<String> actions = new ArrayList<>();

    private String requestedAuthnContextProvider;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(final String entityID) {
        this.entityID = entityID;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    public boolean isCreateUnmatching() {
        return createUnmatching;
    }

    public void setCreateUnmatching(final boolean createUnmatching) {
        this.createUnmatching = createUnmatching;
    }

    public boolean isSelfRegUnmatching() {
        return selfRegUnmatching;
    }

    public void setSelfRegUnmatching(final boolean selfRegUnmatching) {
        this.selfRegUnmatching = selfRegUnmatching;
    }

    public boolean isUpdateMatching() {
        return updateMatching;
    }

    public void setUpdateMatching(final boolean updateMatching) {
        this.updateMatching = updateMatching;
    }

    public SAML2BindingType getBindingType() {
        return bindingType;
    }

    public void setBindingType(final SAML2BindingType bindingType) {
        this.bindingType = bindingType;
    }

    public boolean isLogoutSupported() {
        return logoutSupported;
    }

    public void setLogoutSupported(final boolean logoutSupported) {
        this.logoutSupported = logoutSupported;
    }

    public UserTO getUserTemplate() {
        return userTemplate;
    }

    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = userTemplate;
    }

    @Override
    public ItemTO getConnObjectKeyItem() {
        return getItems().stream().filter(ItemTO::isConnObjectKey).findFirst().orElse(null);
    }

    protected boolean addConnObjectKeyItem(final ItemTO connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return this.add(connObjectItem);
    }

    @Override
    public boolean setConnObjectKeyItem(final ItemTO connObjectKeyItem) {
        return Optional.ofNullable(connObjectKeyItem).
                map(this::addConnObjectKeyItem).orElseGet(() -> items.remove(getConnObjectKeyItem()));
    }

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    @Override
    public List<ItemTO> getItems() {
        return items;
    }

    @Override
    public boolean add(final ItemTO item) {
        return Optional.ofNullable(item).filter(itemTO -> items.contains(itemTO) || items.add(itemTO)).isPresent();
    }

    @JacksonXmlElementWrapper(localName = "actions")
    @JacksonXmlProperty(localName = "action")
    public List<String> getActions() {
        return actions;
    }

    public String getRequestedAuthnContextProvider() {
        return requestedAuthnContextProvider;
    }

    public void setRequestedAuthnContextProvider(final String requestedAuthnContextProvider) {
        this.requestedAuthnContextProvider = requestedAuthnContextProvider;
    }
}
