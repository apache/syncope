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

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.jpa.converters.AuthModuleConfConverter;
import org.apache.syncope.core.persistence.jpa.converters.ItemListConverter;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;

@Entity
@Table(name = JPAAuthModule.TABLE)
public class JPAAuthModule extends AbstractProvidedKeyEntity implements AuthModule {

    private static final long serialVersionUID = 5681033638234853077L;

    public static final String TABLE = "AuthModule";

    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AuthModuleState authModuleState;

    @NotNull
    private Integer authModuleOrder = 0;

    @Convert(converter = ItemListConverter.class)
    @Lob
    private List<Item> items = new ArrayList<>();

    @Convert(converter = AuthModuleConfConverter.class)
    @Lob
    private AuthModuleConf jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public AuthModuleState getState() {
        return authModuleState;
    }

    @Override
    public void setState(final AuthModuleState state) {
        this.authModuleState = state;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(authModuleOrder).orElse(0);
    }

    @Override
    public void setOrder(final int order) {
        this.authModuleOrder = order;
    }

    @Override
    public List<Item> getItems() {
        return items;
    }

    @Override
    public AuthModuleConf getConf() {
        return jsonConf;
    }

    @Override
    public void setConf(final AuthModuleConf conf) {
        jsonConf = conf;
    }
}
