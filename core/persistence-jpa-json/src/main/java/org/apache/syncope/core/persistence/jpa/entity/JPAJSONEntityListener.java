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

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.JSONAttributable;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;

public abstract class JPAJSONEntityListener<A extends Any<?>> {

    protected abstract List<? extends JSONPlainAttr<A>> getAttrs(String plainAttrsJSON);

    @SuppressWarnings("unchecked")
    protected void json2list(final JSONAttributable<A> entity, final boolean clearFirst) {
        if (clearFirst) {
            entity.getPlainAttrList().clear();
        }
        if (entity.getPlainAttrsJSON() != null) {
            getAttrs(entity.getPlainAttrsJSON()).stream().filter(attr -> attr.getSchema() != null).map(attr -> {
                if (entity instanceof Any) {
                    attr.setOwner((A) entity);
                } else if (entity instanceof LinkedAccount) {
                    attr.setOwner((A) ((LinkedAccount) entity).getOwner());
                    ((LAPlainAttr) attr).setAccount((LinkedAccount) entity);
                }
                attr.getValues().forEach(value -> value.setAttr(attr));
                if (attr.getUniqueValue() != null) {
                    attr.getUniqueValue().setAttr(attr);
                }
                return attr;
            }).forEach(entity::add);
        }
    }

    protected void list2json(final JSONAttributable<A> entity) {
        entity.setPlainAttrsJSON(entity.getPlainAttrList().isEmpty()
                ? "[{}]"
                : POJOHelper.serialize(entity.getPlainAttrList()));
    }
}
