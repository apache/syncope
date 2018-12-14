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

@XmlRootElement(name = "orgUnit")
@XmlType
public class OrgUnitTO implements EntityTO, ItemContainerTO {

    private static final long serialVersionUID = -1868877794174953177L;

    private String key;

    private String objectClass;

    private String syncToken;

    private boolean ignoreCaseMatch;

    private String connObjectLink;

    private final List<ItemTO> items = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(final String objectClass) {
        this.objectClass = objectClass;
    }

    public String getSyncToken() {
        return syncToken;
    }

    public void setSyncToken(final String syncToken) {
        this.syncToken = syncToken;
    }

    public boolean isIgnoreCaseMatch() {
        return ignoreCaseMatch;
    }

    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
    }

    public String getConnObjectLink() {
        return connObjectLink;
    }

    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    @Override
    public ItemTO getConnObjectKeyItem() {
        return getItems().stream().filter(item -> item.isConnObjectKey()).findFirst().orElse(null);
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
}
