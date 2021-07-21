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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAttrReleasePolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccessPolicy;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@MappedSuperclass
public class AbstractClientApp extends AbstractGeneratedKeyEntity implements ClientApp {

    private static final long serialVersionUID = 7422422526695279794L;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private Long clientAppId;

    @Column
    private String description;

    @Column
    private String theme;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPARealm realm;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAuthPolicy authPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccessPolicy accessPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAttrReleasePolicy attrReleasePolicy;

    @Lob
    private String properties;

    @Override
    public Long getClientAppId() {
        return clientAppId;
    }

    @Override
    public void setClientAppId(final Long clientAppId) {
        this.clientAppId = clientAppId;
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
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
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
    public List<Attr> getProperties() {
        return properties == null
                ? new ArrayList<>(0)
                : POJOHelper.deserialize(properties, new TypeReference<>() {
        });
    }

    @Override
    public void setProperties(final List<Attr> properties) {
        this.properties = POJOHelper.serialize(properties);
    }
}
