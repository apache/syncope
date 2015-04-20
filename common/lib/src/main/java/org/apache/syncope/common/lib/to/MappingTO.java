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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.IntMappingType;

@XmlRootElement(name = "mapping")
@XmlType
public class MappingTO extends AbstractBaseBean {

    private static final long serialVersionUID = 8447688036282611118L;

    private String accountLink;

    private final List<MappingItemTO> items = new ArrayList<>();

    public String getAccountLink() {
        return accountLink;
    }

    public void setAccountLink(final String accountLink) {
        this.accountLink = accountLink;
    }

    public MappingItemTO getAccountIdItem() {
        return CollectionUtils.find(getItems(), new Predicate<MappingItemTO>() {

            @Override
            public boolean evaluate(final MappingItemTO item) {
                return item.isAccountid();
            }
        });
    }

    protected boolean addAccountIdItem(final MappingItemTO accountIdItem) {
        if (IntMappingType.UserVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.GroupVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.MembershipVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.Password == accountIdItem.getIntMappingType()) {

            throw new IllegalArgumentException("Virtual attributes cannot be set as accountId");
        }
        if (IntMappingType.Password == accountIdItem.getIntMappingType()) {
            throw new IllegalArgumentException("Password attributes cannot be set as accountId");
        }

        accountIdItem.setExtAttrName(accountIdItem.getExtAttrName());
        accountIdItem.setAccountid(true);

        return this.addItem(accountIdItem);
    }

    public boolean setAccountIdItem(final MappingItemTO accountIdItem) {
        return accountIdItem == null
                ? removeItem(getAccountIdItem())
                : addAccountIdItem(accountIdItem);
    }

    public MappingItemTO getPasswordItem() {
        return CollectionUtils.find(getItems(), new Predicate<MappingItemTO>() {

            @Override
            public boolean evaluate(final MappingItemTO item) {
                return item.isPassword();
            }
        });
    }

    public boolean setPasswordItem(final MappingItemTO passwordItem) {
        if (passwordItem == null) {
            return this.removeItem(getPasswordItem());
        } else {
            passwordItem.setExtAttrName(null);
            passwordItem.setPassword(true);
            return addItem(passwordItem);
        }
    }

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    @JsonProperty("items")
    public List<MappingItemTO> getItems() {
        return items;
    }

    public boolean addItem(final MappingItemTO item) {
        return item == null ? false : this.items.contains(item) || this.items.add(item);
    }

    public boolean removeItem(final MappingItemTO item) {
        return this.items.remove(item);
    }
}
