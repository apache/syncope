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
package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.types.LogoutType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccessPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAttrReleasePolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPATicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@MappedSuperclass
public class AbstractClientApp extends AbstractGeneratedKeyEntity implements ClientApp {

    private static final long serialVersionUID = 7422422526695279794L;

    protected static final TypeReference<List<Attr>> ATTR_TYPEREF = new TypeReference<List<Attr>>() {
    };

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private Long clientAppId;

    private int evaluationOrder;

    private String description;

    private String logo;

    @Lob
    private String usernameAttributeProviderConf;

    private String theme;

    private String informationUrl;

    private String privacyUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPARealm realm;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAuthPolicy authPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccessPolicy accessPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAttrReleasePolicy attrReleasePolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPATicketExpirationPolicy ticketExpirationPolicy;

    @Lob
    private String properties;

    @Enumerated(EnumType.STRING)
    private LogoutType logoutType;

    @Override
    public Long getClientAppId() {
        return clientAppId;
    }

    @Override
    public void setClientAppId(final Long clientAppId) {
        this.clientAppId = clientAppId;
    }

    @Override
    public int getEvaluationOrder() {
        return evaluationOrder;
    }

    @Override
    public void setEvaluationOrder(final int evaluationOrder) {
        this.evaluationOrder = evaluationOrder;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getLogo() {
        return logo;
    }

    @Override
    public void setLogo(final String logo) {
        this.logo = logo;
    }

    @Override
    public String getTheme() {
        return theme;
    }

    @Override
    public void setTheme(final String theme) {
        this.theme = theme;
    }

    @Override
    public String getInformationUrl() {
        return informationUrl;
    }

    @Override
    public void setInformationUrl(final String informationUrl) {
        this.informationUrl = informationUrl;
    }

    @Override
    public String getPrivacyUrl() {
        return privacyUrl;
    }

    @Override
    public void setPrivacyUrl(final String privacyUrl) {
        this.privacyUrl = privacyUrl;
    }

    @Override
    public UsernameAttributeProviderConf getUsernameAttributeProviderConf() {
        return Optional.ofNullable(usernameAttributeProviderConf).
                map(conf -> POJOHelper.deserialize(conf, UsernameAttributeProviderConf.class)).orElse(null);
    }

    @Override
    public void setUsernameAttributeProviderConf(final UsernameAttributeProviderConf conf) {
        this.usernameAttributeProviderConf = conf == null ? null : POJOHelper.serialize(conf);
    }

    @Override
    public JPAAuthPolicy getAuthPolicy() {
        return authPolicy;
    }

    @Override
    public void setAuthPolicy(final AuthPolicy authPolicy) {
        checkType(authPolicy, JPAAuthPolicy.class);
        this.authPolicy = (JPAAuthPolicy) authPolicy;
    }

    @Override
    public JPAAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public void setAccessPolicy(final AccessPolicy accessPolicy) {
        checkType(accessPolicy, JPAAccessPolicy.class);
        this.accessPolicy = (JPAAccessPolicy) accessPolicy;
    }

    @Override
    public AttrReleasePolicy getAttrReleasePolicy() {
        return this.attrReleasePolicy;
    }

    @Override
    public void setAttrReleasePolicy(final AttrReleasePolicy policy) {
        checkType(policy, JPAAttrReleasePolicy.class);
        this.attrReleasePolicy = (JPAAttrReleasePolicy) policy;
    }

    @Override
    public TicketExpirationPolicy getTicketExpirationPolicy() {
        return this.ticketExpirationPolicy;
    }

    @Override
    public void setTicketExpirationPolicy(final TicketExpirationPolicy policy) {
        checkType(policy, JPATicketExpirationPolicy.class);
        this.ticketExpirationPolicy = (JPATicketExpirationPolicy) policy;
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
    }

    @Override
    public List<Attr> getProperties() {
        return properties == null
                ? new ArrayList<>(0)
                : POJOHelper.deserialize(properties, ATTR_TYPEREF);
    }

    @Override
    public void setProperties(final List<Attr> properties) {
        this.properties = POJOHelper.serialize(properties);
    }

    @Override
    public LogoutType getLogoutType() {
        return this.logoutType;
    }

    @Override
    public void setLogoutType(final LogoutType logoutType) {
        this.logoutType = logoutType;
    }
}
