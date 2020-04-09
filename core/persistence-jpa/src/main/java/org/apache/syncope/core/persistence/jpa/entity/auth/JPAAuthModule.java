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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAuthModule.TABLE)
public class JPAAuthModule extends AbstractGeneratedKeyEntity implements AuthModule {

    public static final String TABLE = "AuthModule";

    private static final long serialVersionUID = 5681033638234853077L;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "mapping")
    private final List<JPAAuthModuleItem> profileItems = new ArrayList<>();

    @Lob
    private String jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
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
    public List<? extends Item> getProfileItems() {
        return profileItems;
    }

    @Override
    public boolean add(final Item profileItem) {
        checkType(profileItem, JPAAuthModuleItem.class);
        return profileItems.contains((JPAAuthModuleItem) profileItem)
                || profileItems.add((JPAAuthModuleItem) profileItem);
    }

    @Override
    public AuthModuleConf getConf() {
        AuthModuleConf conf = null;
        if (!StringUtils.isBlank(jsonConf)) {
            conf = POJOHelper.deserialize(jsonConf, AuthModuleConf.class);
        }

        return conf;
    }

    @Override
    public void setConf(final AuthModuleConf conf) {
        jsonConf = POJOHelper.serialize(conf);
    }

}
