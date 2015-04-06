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
package org.apache.syncope.core.persistence.jpa.entity;

import javax.persistence.Cacheable;
import javax.persistence.MappedSuperclass;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.core.persistence.api.entity.Mapping;
import org.apache.syncope.core.persistence.api.entity.MappingItem;

@MappedSuperclass
@Cacheable
public abstract class AbstractMapping<T extends MappingItem> extends AbstractEntity<Long> implements Mapping<T> {

    private static final long serialVersionUID = 4316047254916259158L;

    /**
     * A JEXL expression for determining how to find the account id in external resource's space.
     */
    private String accountLink;

    @Override
    public String getAccountLink() {
        return accountLink;
    }

    @Override
    public void setAccountLink(final String accountLink) {
        this.accountLink = accountLink;
    }

    @Override
    public T getAccountIdItem() {
        T accountIdItem = null;
        for (T item : getItems()) {
            if (item.isAccountid()) {
                accountIdItem = item;
            }
        }
        return accountIdItem;
    }

    protected boolean addAccountIdItem(final T accountIdItem) {
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
}
