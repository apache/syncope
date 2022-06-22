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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepoItem;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAttrRepo.TABLE)
public class JPAAttrRepo extends AbstractProvidedKeyEntity implements AttrRepo {

    public static final String TABLE = "AttrRepo";

    private static final long serialVersionUID = 7337970107878689617L;

    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AttrRepoState attrRepoState;

    @NotNull
    private Integer attrRepoOrder = 0;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "attrRepo")
    private List<JPAAttrRepoItem> items = new ArrayList<>();

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
    public AttrRepoState getState() {
        return attrRepoState;
    }

    @Override
    public void setState(final AttrRepoState state) {
        this.attrRepoState = state;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(attrRepoOrder).orElse(0);
    }

    @Override
    public void setOrder(final int order) {
        this.attrRepoOrder = order;
    }

    @Override
    public List<? extends AttrRepoItem> getItems() {
        return items;
    }

    @Override
    public boolean add(final AttrRepoItem item) {
        checkType(item, JPAAttrRepoItem.class);
        return items.contains((JPAAttrRepoItem) item) || items.add((JPAAttrRepoItem) item);
    }

    @Override
    public AttrRepoConf getConf() {
        AttrRepoConf conf = null;
        if (!StringUtils.isBlank(jsonConf)) {
            conf = POJOHelper.deserialize(jsonConf, AttrRepoConf.class);
        }

        return conf;
    }

    @Override
    public void setConf(final AttrRepoConf conf) {
        jsonConf = POJOHelper.serialize(conf);
    }
}
