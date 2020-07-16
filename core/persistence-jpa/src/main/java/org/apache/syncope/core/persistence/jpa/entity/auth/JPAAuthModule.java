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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModuleItem;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAuthModule.TABLE)
public class JPAAuthModule extends AbstractProvidedKeyEntity implements AuthModule {

    public static final String TABLE = "AuthModule";

    private static final long serialVersionUID = 5681033638234853077L;

    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "authModule")
    private List<JPAAuthModuleItem> items = new ArrayList<>();

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
    public List<? extends AuthModuleItem> getItems() {
        return items;
    }

    @Override
    public boolean add(final AuthModuleItem item) {
        checkType(item, JPAAuthModuleItem.class);
        return items.contains((JPAAuthModuleItem) item) || items.add((JPAAuthModuleItem) item);
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
