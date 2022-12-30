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
package org.apache.syncope.common.lib.to;

import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.types.AttrRepoState;

public class AttrRepoTO implements EntityTO {

    private static final long serialVersionUID = -7490425997956703057L;

    private String key;

    private String description;

    private AttrRepoState state = AttrRepoState.ACTIVE;

    private int order = 0;

    private final List<Item> items = new ArrayList<>();

    private AttrRepoConf conf;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public AttrRepoState getState() {
        return state;
    }

    public void setState(final AttrRepoState state) {
        this.state = state;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public List<Item> getItems() {
        return items;
    }

    public AttrRepoConf getConf() {
        return conf;
    }

    public void setConf(final AttrRepoConf conf) {
        this.conf = conf;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttrRepoTO other = (AttrRepoTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(description, other.description).
                append(state, other.state).
                append(order, other.order).
                append(items, other.items).
                append(conf, other.conf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(description).
                append(state).
                append(order).
                append(items).
                append(conf).
                build();
    }
}
