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
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.syncope.core.persistence.api.entity.JSONAttributable;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.validation.entity.JPAJSONAttributableCheck;

@Entity
@Table(name = JPALinkedAccount.TABLE)
@EntityListeners({ JPAJSONLinkedAccountListener.class })
@JPAJSONAttributableCheck
public class JPAJSONLinkedAccount extends JPALinkedAccount implements JSONAttributable<User>, LinkedAccount {

    private static final long serialVersionUID = 7495284980208765032L;

    @Lob
    private String plainAttrs;

    @Transient
    private final List<JPAJSONLAPlainAttr> plainAttrList = new ArrayList<>();

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public List<JPAJSONLAPlainAttr> getPlainAttrList() {
        return plainAttrList;
    }

    @Override
    public boolean add(final JSONPlainAttr<User> attr) {
        return add((LAPlainAttr) attr);
    }

    @Override
    public boolean add(final LAPlainAttr attr) {
        checkType(attr, JPAJSONLAPlainAttr.class);
        return plainAttrList.add((JPAJSONLAPlainAttr) attr);
    }

    @Override
    public boolean remove(final LAPlainAttr attr) {
        return plainAttrList.removeIf(jsonAttr -> jsonAttr.getSchemaKey().equals(attr.getSchema().getKey()));
    }

    @Override
    public List<? extends LAPlainAttr> getPlainAttrs() {
        return plainAttrList.stream().collect(Collectors.toList());
    }

    @Override
    public Optional<? extends LAPlainAttr> getPlainAttr(final String plainSchema) {
        return plainAttrList.stream().
                filter(attr -> attr.getSchemaKey() != null && attr.getSchemaKey().equals(plainSchema)).
                findFirst();
    }
}
