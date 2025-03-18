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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
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
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Relationship;

public abstract class AbstractClientApp extends AbstractGeneratedKeyNode implements ClientApp {

    private static final long serialVersionUID = 7422422526695279794L;

    protected static final TypeReference<List<Attr>> ATTR_TYPEREF = new TypeReference<List<Attr>>() {
    };

    @NotNull
    private String name;

    @NotNull
    private Long clientAppId;

    private int evaluationOrder;

    private String description;

    private String logo;

    private String usernameAttributeProviderConf;

    private String theme;

    private String informationUrl;

    private String privacyUrl;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm realm;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAuthPolicy authPolicy;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAccessPolicy accessPolicy;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jAttrReleasePolicy attrReleasePolicy;

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jTicketExpirationPolicy ticketExpirationPolicy;

    private String properties;

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
    public Neo4jAuthPolicy getAuthPolicy() {
        return authPolicy;
    }

    @Override
    public void setAuthPolicy(final AuthPolicy authPolicy) {
        checkType(authPolicy, Neo4jAuthPolicy.class);
        this.authPolicy = (Neo4jAuthPolicy) authPolicy;
    }

    @Override
    public Neo4jAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public void setAccessPolicy(final AccessPolicy accessPolicy) {
        checkType(accessPolicy, Neo4jAccessPolicy.class);
        this.accessPolicy = (Neo4jAccessPolicy) accessPolicy;
    }

    @Override
    public AttrReleasePolicy getAttrReleasePolicy() {
        return this.attrReleasePolicy;
    }

    @Override
    public void setAttrReleasePolicy(final AttrReleasePolicy policy) {
        checkType(policy, Neo4jAttrReleasePolicy.class);
        this.attrReleasePolicy = (Neo4jAttrReleasePolicy) policy;
    }

    @Override
    public TicketExpirationPolicy getTicketExpirationPolicy() {
        return this.ticketExpirationPolicy;
    }

    @Override
    public void setTicketExpirationPolicy(final TicketExpirationPolicy policy) {
        checkType(policy, Neo4jTicketExpirationPolicy.class);
        this.ticketExpirationPolicy = (Neo4jTicketExpirationPolicy) policy;
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, Neo4jRealm.class);
        this.realm = (Neo4jRealm) realm;
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
