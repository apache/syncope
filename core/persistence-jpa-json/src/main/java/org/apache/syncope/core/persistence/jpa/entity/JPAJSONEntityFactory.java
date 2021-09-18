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

import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAJSONAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAJSONAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAJSONAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAJSONAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAJSONGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAJSONGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAJSONGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAJSONGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONLAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONLAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONLAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONLinkedAccount;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUser;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUPlainAttrValue;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

public abstract class JPAJSONEntityFactory extends JPAEntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(User.class)) {
            result = (E) new JPAJSONUser();
            ((JPAJSONUser) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(LinkedAccount.class)) {
            result = (E) new JPAJSONLinkedAccount();
            ((JPAJSONLinkedAccount) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(Group.class)) {
            result = (E) new JPAJSONGroup();
            ((JPAJSONGroup) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(AnyObject.class)) {
            result = (E) new JPAJSONAnyObject();
            ((JPAJSONAnyObject) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(APlainAttr.class)) {
            result = (E) new JPAJSONAPlainAttr();
        } else if (reference.equals(APlainAttrValue.class)) {
            result = (E) new JPAJSONAPlainAttrValue();
        } else if (reference.equals(APlainAttrUniqueValue.class)) {
            result = (E) new JPAJSONAPlainAttrUniqueValue();
        } else if (reference.equals(GPlainAttr.class)) {
            result = (E) new JPAJSONGPlainAttr();
        } else if (reference.equals(GPlainAttrValue.class)) {
            result = (E) new JPAJSONGPlainAttrValue();
        } else if (reference.equals(GPlainAttrUniqueValue.class)) {
            result = (E) new JPAJSONGPlainAttrUniqueValue();
        } else if (reference.equals(UPlainAttr.class)) {
            result = (E) new JPAJSONUPlainAttr();
        } else if (reference.equals(UPlainAttrValue.class)) {
            result = (E) new JPAJSONUPlainAttrValue();
        } else if (reference.equals(UPlainAttrUniqueValue.class)) {
            result = (E) new JPAJSONUPlainAttrUniqueValue();
        } else if (reference.equals(LAPlainAttr.class)) {
            result = (E) new JPAJSONLAPlainAttr();
        } else if (reference.equals(LAPlainAttrValue.class)) {
            result = (E) new JPAJSONLAPlainAttrValue();
        } else if (reference.equals(LAPlainAttrUniqueValue.class)) {
            result = (E) new JPAJSONLAPlainAttrUniqueValue();
        } else {
            result = super.newEntity(reference);
        }

        return result;
    }

    @Override
    public Class<? extends User> userClass() {
        return JPAJSONUser.class;
    }

    @Override
    public Class<? extends Group> groupClass() {
        return JPAJSONGroup.class;
    }

    @Override
    public Class<? extends AnyObject> anyObjectClass() {
        return JPAJSONAnyObject.class;
    }
}
