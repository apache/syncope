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

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.persistence.neo4j.converters.AttrRepoConfConverter;
import org.apache.syncope.core.persistence.neo4j.converters.ItemListConverter;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jAttrRepo.NODE)
public class Neo4jAttrRepo extends AbstractProvidedKeyNode implements AttrRepo {

    private static final long serialVersionUID = 7337970107878689617L;

    public static final String NODE = "AttrRepo";

    private String description;

    @NotNull
    private AttrRepoState attrRepoState;

    @NotNull
    private Integer attrRepoOrder = 0;

    @ConvertWith(converter = ItemListConverter.class)
    private List<Item> items = new ArrayList<>();

    @ConvertWith(converter = AttrRepoConfConverter.class)
    private AttrRepoConf jsonConf;

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
    public List<Item> getItems() {
        return items;
    }

    @Override
    public AttrRepoConf getConf() {
        return jsonConf;
    }

    @Override
    public void setConf(final AttrRepoConf conf) {
        jsonConf = conf;
    }
}
