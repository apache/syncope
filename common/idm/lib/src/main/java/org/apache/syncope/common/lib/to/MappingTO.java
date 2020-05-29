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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MappingTO implements ItemContainerTO {

    private static final long serialVersionUID = 8447688036282611118L;

    private String connObjectLink;

    private final List<ItemTO> items = new ArrayList<>();

    private final List<ItemTO> linkingItems = new ArrayList<>();

    public String getConnObjectLink() {
        return connObjectLink;
    }

    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    @Override
    public ItemTO getConnObjectKeyItem() {
        return getItems().stream().filter(ItemTO::isConnObjectKey).findFirst().orElse(null);
    }

    protected boolean addConnObjectKeyItem(final ItemTO connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return this.add(connObjectItem);
    }

    @Override
    public boolean setConnObjectKeyItem(final ItemTO connObjectKeyItem) {
        return Optional.ofNullable(connObjectKeyItem)
                .map(this::addConnObjectKeyItem).orElseGet(() -> remove(getConnObjectKeyItem()));
    }

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    @Override
    public List<ItemTO> getItems() {
        return items;
    }

    @Override
    public boolean add(final ItemTO item) {
        return Optional.ofNullable(item)
                .filter(itemTO -> this.items.contains(itemTO) || this.items.add(itemTO)).isPresent();
    }

    public boolean remove(final ItemTO item) {
        return this.items.remove(item);
    }

    @JacksonXmlElementWrapper(localName = "linkingItems")
    @JacksonXmlProperty(localName = "linkingItem")
    public List<ItemTO> getLinkingItems() {
        return linkingItems;
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
        MappingTO other = (MappingTO) obj;
        return new EqualsBuilder().
                append(connObjectLink, other.connObjectLink).
                append(items, other.items).
                append(linkingItems, other.linkingItems).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(connObjectLink).
                append(items).
                append(linkingItems).
                build();
    }
}
