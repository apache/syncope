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
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPAUPlainAttr.TABLE)
public class JPAUPlainAttr extends AbstractPlainAttr<User> implements UPlainAttr {

    private static final long serialVersionUID = 6333601983691157406L;

    public static final String TABLE = "UPlainAttr";

    /**
     * The owner of this attribute.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private JPAUser owner;

    /**
     * The membership of this attribute; might be {@code NULL} if this attribute is not related to a membership.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private JPAUMembership membership;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPAUPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private JPAUPlainAttrUniqueValue uniqueValue;

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
    public UMembership getMembership() {
        return membership;
    }

    @Override
    public void setMembership(final UMembership membership) {
        checkType(membership, JPAUMembership.class);
        this.membership = (JPAUMembership) membership;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAUPlainAttrValue.class);
        return values.add((JPAUPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends UPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public UPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPAUPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAUPlainAttrUniqueValue) uniqueValue;
    }
}
