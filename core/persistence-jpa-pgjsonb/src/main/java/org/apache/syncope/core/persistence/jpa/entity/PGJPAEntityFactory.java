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

import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.dao.PGJPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.PGAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.PGAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.PGAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.PGJPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.conf.PGCPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.conf.PGCPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.PGCPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.PGJPAConf;
import org.apache.syncope.core.persistence.jpa.entity.group.PGGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.PGGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.PGGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.PGJPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.PGJPAUser;
import org.apache.syncope.core.persistence.jpa.entity.user.PGUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.PGUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.PGUPlainAttrValue;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

public class PGJPAEntityFactory extends JPAEntityFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(User.class)) {
            result = (E) new PGJPAUser();
            ((PGJPAUser) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(Group.class)) {
            result = (E) new PGJPAGroup();
            ((PGJPAGroup) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(AnyObject.class)) {
            result = (E) new PGJPAAnyObject();
            ((PGJPAAnyObject) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(Conf.class)) {
            result = (E) new PGJPAConf();
            ((PGJPAConf) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        } else if (reference.equals(APlainAttr.class)) {
            result = (E) new PGAPlainAttr();
        } else if (reference.equals(APlainAttrValue.class)) {
            result = (E) new PGAPlainAttrValue();
        } else if (reference.equals(APlainAttrUniqueValue.class)) {
            result = (E) new PGAPlainAttrUniqueValue();
        } else if (reference.equals(CPlainAttr.class)) {
            result = (E) new PGCPlainAttr();
        } else if (reference.equals(CPlainAttrValue.class)) {
            result = (E) new PGCPlainAttrValue();
        } else if (reference.equals(CPlainAttrUniqueValue.class)) {
            result = (E) new PGCPlainAttrUniqueValue();
        } else if (reference.equals(GPlainAttr.class)) {
            result = (E) new PGGPlainAttr();
        } else if (reference.equals(GPlainAttrValue.class)) {
            result = (E) new PGGPlainAttrValue();
        } else if (reference.equals(GPlainAttrUniqueValue.class)) {
            result = (E) new PGGPlainAttrUniqueValue();
        } else if (reference.equals(UPlainAttr.class)) {
            result = (E) new PGUPlainAttr();
        } else if (reference.equals(UPlainAttrValue.class)) {
            result = (E) new PGUPlainAttrValue();
        } else if (reference.equals(UPlainAttrUniqueValue.class)) {
            result = (E) new PGUPlainAttrUniqueValue();
        } else {
            result = super.newEntity(reference);
        }

        return result;
    }

    @Override
    public Class<? extends User> userClass() {
        return PGJPAUser.class;
    }

    @Override
    public Class<? extends Group> groupClass() {
        return PGJPAGroup.class;
    }

    @Override
    public Class<? extends AnyObject> anyObjectClass() {
        return PGJPAAnyObject.class;
    }

    @Override
    public Class<? extends Conf> confClass() {
        return PGJPAConf.class;
    }

    @Override
    public Class<? extends AnySearchDAO> anySearchDAOClass() {
        return PGJPAAnySearchDAO.class;
    }
}
