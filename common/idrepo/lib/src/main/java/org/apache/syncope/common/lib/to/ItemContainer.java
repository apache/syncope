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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class ItemContainer implements Serializable {

    private static final long serialVersionUID = 3637981417797873343L;

    private final List<Item> items = new ArrayList<>();

    @JsonIgnore
    public Optional<Item> getConnObjectKeyItem() {
        return getItems().stream().filter(Item::isConnObjectKey).findFirst();
    }

    protected boolean addConnObjectKeyItem(final Item connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return add(connObjectItem);
    }

    public boolean setConnObjectKeyItem(final Item connObjectKeyItem) {
        return Optional.ofNullable(connObjectKeyItem).
                map(this::addConnObjectKeyItem).
                orElseGet(() -> getConnObjectKeyItem().map(items::remove).orElse(false));
    }

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    public List<Item> getItems() {
        return items;
    }

    public boolean add(final Item item) {
        return Optional.ofNullable(item).
                filter(itemTO -> items.contains(itemTO) || items.add(itemTO)).isPresent();
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
        final ItemContainer other = (ItemContainer) obj;
        return new EqualsBuilder().
                append(items, other.items).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(items).
                build();
    }
}
