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
package org.apache.syncope.core.persistence.jpa.entity.user;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPALAPlainAttr.TABLE)
public class JPALAPlainAttr extends AbstractPlainAttr<User> implements LAPlainAttr {

    private static final long serialVersionUID = 7827533741035423694L;

    public static final String TABLE = "LAPlainAttr";

    /**
     * The owner of this attribute.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private JPAUser owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPALinkedAccount account;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPALAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private JPALAPlainAttrUniqueValue uniqueValue;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, JPAUser.class);
        this.owner = (JPAUser) owner;
    }

    @Override
    public LinkedAccount getAccount() {
        return account;
    }

    @Override
    public void setAccount(final LinkedAccount account) {
        checkType(account, JPALinkedAccount.class);
        this.account = (JPALinkedAccount) account;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPALAPlainAttrValue.class);
        return values.add((JPALAPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends LAPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public LAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPALAPlainAttrUniqueValue.class);
        this.uniqueValue = (JPALAPlainAttrUniqueValue) uniqueValue;
    }
}
