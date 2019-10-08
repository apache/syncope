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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.JSONAttributable;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.jpa.validation.entity.JPAJSONAttributableCheck;

@Entity
@Table(name = JPAUser.TABLE)
@EntityListeners({ JPAJSONUserListener.class })
@JPAJSONAttributableCheck
public class JPAJSONUser extends JPAUser implements JSONAttributable<User>, User {

    private static final long serialVersionUID = -8543654943709531885L;

    @Lob
    private String plainAttrs;

    @Transient
    private final List<JPAJSONUPlainAttr> plainAttrList = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    @Valid
    private List<JPAJSONLinkedAccount> linkedAccounts = new ArrayList<>();

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public List<JPAJSONUPlainAttr> getPlainAttrList() {
        return plainAttrList;
    }

    @Override
    public boolean add(final JSONPlainAttr<User> attr) {
        return add((UPlainAttr) attr);
    }

    @Override
    public boolean add(final UPlainAttr attr) {
        checkType(attr, JPAJSONUPlainAttr.class);
        return plainAttrList.add((JPAJSONUPlainAttr) attr);
    }

    @Override
    public boolean remove(final UPlainAttr attr) {
        return plainAttrList.removeIf(jsonAttr -> jsonAttr.getSchemaKey().equals(attr.getSchema().getKey())
                && Objects.equals(jsonAttr.getMembershipKey(), ((JPAJSONUPlainAttr) attr).getMembershipKey()));
    }

    @Override
    protected List<? extends UPlainAttr> internalGetPlainAttrs() {
        return plainAttrList;
    }

    @Override
    public List<? extends UPlainAttr> getPlainAttrs() {
        return plainAttrList.stream().
                filter(attr -> attr.getMembershipKey() == null).
                collect(Collectors.toList());
    }

    @Override
    public Optional<? extends UPlainAttr> getPlainAttr(final String plainSchema) {
        return plainAttrList.stream().
                filter(attr -> attr.getSchemaKey() != null && attr.getSchemaKey().equals(plainSchema)
                && attr.getMembershipKey() == null).
                findFirst();
    }

    @Override
    public Optional<? extends UPlainAttr> getPlainAttr(final String plainSchema, final Membership<?> membership) {
        return plainAttrList.stream().
                filter(attr -> attr.getSchemaKey() != null && attr.getSchemaKey().equals(plainSchema)
                && attr.getMembershipKey() != null && attr.getMembershipKey().equals(membership.getKey())).
                findFirst();
    }

    @Override
    public boolean remove(final UMembership membership) {
        plainAttrList.removeIf(attr -> attr.getMembershipKey() != null
                && attr.getMembershipKey().equals(membership.getKey()));
        return super.remove(membership);
    }

    @Override
    public boolean add(final LinkedAccount account) {
        checkType(account, JPALinkedAccount.class);
        return linkedAccounts.contains((JPAJSONLinkedAccount) account)
                || linkedAccounts.add((JPAJSONLinkedAccount) account);
    }

    @Override
    public Optional<? extends LinkedAccount> getLinkedAccount(final String resource, final String connObjectKeyValue) {
        return linkedAccounts.stream().
                filter(account -> account.getResource().getKey().equals(resource)
                && account.getConnObjectKeyValue().equals(connObjectKeyValue)).
                findFirst();
    }

    @Override
    public List<? extends LinkedAccount> getLinkedAccounts(final String resource) {
        return linkedAccounts.stream().
                filter(account -> account.getResource().getKey().equals(resource)).
                collect(Collectors.toList());
    }

    @Override
    public List<? extends LinkedAccount> getLinkedAccounts() {
        return linkedAccounts;
    }
}
