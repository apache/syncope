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
package org.apache.syncope.core.persistence.beans;

import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.MappedSuperclass;
import org.apache.syncope.types.IntMappingType;
import org.identityconnectors.framework.common.objects.Uid;

@MappedSuperclass
@Cacheable
public abstract class AbstractMapping extends AbstractBaseBean {

    private static final long serialVersionUID = 4316047254916259158L;

    /**
     * A JEXL expression for determining how to find the account id in external resource's space.
     */
    private String accountLink;

    public abstract Long getId();

    public abstract void setResource(ExternalResource resource);

    public abstract ExternalResource getResource();

    public String getAccountLink() {
        return accountLink;
    }

    public void setAccountLink(final String accountLink) {
        this.accountLink = accountLink;
    }

    public <T extends AbstractMappingItem> T getAccountIdItem() {
        T accountIdItem = null;
        for (AbstractMappingItem item : getItems()) {
            if (item.isAccountid()) {
                accountIdItem = (T) item;
            }
        }
        return accountIdItem;
    }

    protected <T extends AbstractMappingItem> boolean addAccountIdItem(final T accountIdItem) {
        if (IntMappingType.UserVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.RoleVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.MembershipVirtualSchema == accountIdItem.getIntMappingType()
                || IntMappingType.Password == accountIdItem.getIntMappingType()) {

            throw new IllegalArgumentException("Virtual attributes cannot be set as accountId");
        }
        if (IntMappingType.Password == accountIdItem.getIntMappingType()) {
            throw new IllegalArgumentException("Password attributes cannot be set as accountId");
        }

        accountIdItem.setExtAttrName(Uid.NAME);
        accountIdItem.setAccountid(true);

        return this.addItem(accountIdItem);
    }

    public abstract <T extends AbstractMappingItem> void setAccountIdItem(final T accountIdItem);

    public abstract <T extends AbstractMappingItem> List<T> getItems();

    public abstract <T extends AbstractMappingItem> boolean addItem(T item);

    public abstract <T extends AbstractMappingItem> boolean removeItem(T item);

    public abstract <T extends AbstractMappingItem> void setItems(final List<T> items);
}
