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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "mapping")
@XmlType
public class MappingTO extends AbstractBaseBean implements ItemContainerTO {

    private static final long serialVersionUID = 8447688036282611118L;

    private String connObjectLink;

    private final List<ItemTO> items = new ArrayList<>();

    private final List<ItemTO> linkingItems = new ArrayList<>();

    public String getConnObjectLink() {
        return connObjectLink;
    }

    @Override
    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    public ItemTO getConnObjectKeyItem() {
        return IterableUtils.find(getItems(), new Predicate<ItemTO>() {

            @Override
            public boolean evaluate(final ItemTO item) {
                return item.isConnObjectKey();
            }
        });
    }

    protected boolean addConnObjectKeyItem(final ItemTO connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return this.add(connObjectItem);
    }

    @Override
    public boolean setConnObjectKeyItem(final ItemTO connObjectKeyItem) {
        return connObjectKeyItem == null
                ? remove(getConnObjectKeyItem())
                : addConnObjectKeyItem(connObjectKeyItem);
    }

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    @JsonProperty("items")
    @Override
    public List<ItemTO> getItems() {
        return items;
    }

    @Override
    public boolean add(final ItemTO item) {
        return item == null ? false : this.items.contains(item) || this.items.add(item);
    }

    public boolean remove(final ItemTO item) {
        return this.items.remove(item);
    }

    @XmlElementWrapper(name = "linkingItems")
    @XmlElement(name = "item")
    @JsonProperty("linkingItems")
    public List<ItemTO> getLinkingItems() {
        return linkingItems;
    }
}
