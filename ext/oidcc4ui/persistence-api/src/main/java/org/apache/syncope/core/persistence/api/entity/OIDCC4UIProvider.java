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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.Item;

public interface OIDCC4UIProvider extends Entity {

    String getName();

    void setName(String entityID);

    String getClientID();

    void setClientID(String clientId);

    String getClientSecret();

    void setClientSecret(String clientSecret);

    String getAuthorizationEndpoint();

    void setAuthorizationEndpoint(String authorizationEndpoint);

    String getTokenEndpoint();

    void setTokenEndpoint(String tokenEndpoint);

    String getJwksUri();

    void setJwksUri(String jwsUri);

    String getIssuer();

    void setIssuer(String issuer);

    String getUserinfoEndpoint();

    void setUserinfoEndpoint(String userinfoEndpoint);

    String getEndSessionEndpoint();

    void setEndSessionEndpoint(String endSessionEndpoint);

    List<String> getScopes();

    void setScopes(List<String> scopes);

    boolean getHasDiscovery();

    void setHasDiscovery(boolean hasDiscovery);

    boolean isCreateUnmatching();

    void setCreateUnmatching(boolean createUnmatching);

    boolean isSelfRegUnmatching();

    void setSelfRegUnmatching(boolean selfRegUnmatching);

    boolean isUpdateMatching();

    void setUpdateMatching(boolean updateMatching);

    OIDCC4UIUserTemplate getUserTemplate();

    void setUserTemplate(OIDCC4UIUserTemplate userTemplate);

    Optional<Item> getConnObjectKeyItem();

    void setConnObjectKeyItem(Item item);

    List<Item> getItems();

    boolean add(Implementation action);

    List<? extends Implementation> getActions();
}
