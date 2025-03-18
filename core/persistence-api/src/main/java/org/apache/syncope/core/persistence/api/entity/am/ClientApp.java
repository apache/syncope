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
package org.apache.syncope.core.persistence.api.entity.am;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.types.LogoutType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;

public interface ClientApp extends Entity {

    String getName();

    void setName(String name);

    Long getClientAppId();

    void setClientAppId(Long clientAppId);

    int getEvaluationOrder();

    void setEvaluationOrder(int evaluationOrder);

    String getDescription();

    void setDescription(String description);

    String getLogo();

    void setLogo(String logo);

    void setTheme(String name);

    String getTheme();

    String getInformationUrl();

    void setInformationUrl(String informationUrl);

    String getPrivacyUrl();

    void setPrivacyUrl(String privacyUrl);

    UsernameAttributeProviderConf getUsernameAttributeProviderConf();

    void setUsernameAttributeProviderConf(UsernameAttributeProviderConf conf);

    AuthPolicy getAuthPolicy();

    void setAuthPolicy(AuthPolicy policy);

    AccessPolicy getAccessPolicy();

    void setAccessPolicy(AccessPolicy policy);

    AttrReleasePolicy getAttrReleasePolicy();

    void setAttrReleasePolicy(AttrReleasePolicy policy);

    TicketExpirationPolicy getTicketExpirationPolicy();

    void setTicketExpirationPolicy(TicketExpirationPolicy policy);

    Realm getRealm();

    void setRealm(Realm realm);

    List<Attr> getProperties();

    void setProperties(List<Attr> properties);

    LogoutType getLogoutType();

    void setLogoutType(LogoutType logoutType);
}
