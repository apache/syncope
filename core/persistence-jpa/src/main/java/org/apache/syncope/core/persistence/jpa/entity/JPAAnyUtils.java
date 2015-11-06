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

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JPAAnyUtils implements AnyUtils {

    private final AnyTypeKind anyTypeKind;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    protected JPAAnyUtils(final AnyTypeKind typeKind) {
        this.anyTypeKind = typeKind;
    }

    @Override
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public <T extends Any<?>> Class<T> anyClass() {
        Class result;

        switch (anyTypeKind) {
            case GROUP:
                result = JPAGroup.class;
                break;

            case ANY_OBJECT:
                result = JPAAnyObject.class;
                break;

            case USER:
            default:
                result = JPAUser.class;
        }

        return result;
    }

    @Override
    public <T extends PlainAttr<?>> Class<T> plainAttrClass() {
        Class result = null;

        switch (anyTypeKind) {
            case GROUP:
                result = JPAGPlainAttr.class;
                break;

            case ANY_OBJECT:
                result = JPAAPlainAttr.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttr.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttr<?>> T newPlainAttr() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = (T) new JPAUPlainAttr();
                break;

            case GROUP:
                result = (T) new JPAGPlainAttr();
                break;

            case ANY_OBJECT:
                result = (T) new JPAAPlainAttr();
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> Class<T> plainAttrValueClass() {
        Class result;

        switch (anyTypeKind) {
            case GROUP:
                result = JPAGPlainAttrValue.class;
                break;

            case ANY_OBJECT:
                result = JPAAPlainAttrValue.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttrValue.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrValue() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = (T) new JPAUPlainAttrValue();
                break;

            case GROUP:
                result = (T) new JPAGPlainAttrValue();
                break;

            case ANY_OBJECT:
                result = (T) new JPAAPlainAttrValue();
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> Class<T> plainAttrUniqueValueClass() {
        Class result;

        switch (anyTypeKind) {
            case GROUP:
                result = JPAGPlainAttrUniqueValue.class;
                break;

            case ANY_OBJECT:
                result = JPAAPlainAttrUniqueValue.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttrUniqueValue.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrUniqueValue() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = (T) new JPAUPlainAttrUniqueValue();
                break;

            case GROUP:
                result = (T) new JPAGPlainAttrUniqueValue();
                break;

            case ANY_OBJECT:
                result = (T) new JPAAPlainAttrUniqueValue();
                break;

            default:
        }

        return result;
    }

    @Override
    public IntMappingType plainIntMappingType() {
        IntMappingType result = null;

        switch (anyTypeKind) {
            case GROUP:
                result = IntMappingType.GroupPlainSchema;
                break;

            case ANY_OBJECT:
                result = IntMappingType.AnyObjectPlainSchema;
                break;

            case USER:
                result = IntMappingType.UserPlainSchema;
                break;

            default:
        }

        return result;
    }

    @Override
    public IntMappingType derIntMappingType() {
        IntMappingType result = null;

        switch (anyTypeKind) {
            case GROUP:
                result = IntMappingType.GroupDerivedSchema;
                break;

            case ANY_OBJECT:
                result = IntMappingType.AnyObjectDerivedSchema;
                break;

            case USER:
                result = IntMappingType.UserDerivedSchema;
                break;

            default:
        }

        return result;
    }

    @Override
    public IntMappingType virIntMappingType() {
        IntMappingType result = null;

        switch (anyTypeKind) {
            case GROUP:
                result = IntMappingType.GroupVirtualSchema;
                break;

            case ANY_OBJECT:
                result = IntMappingType.AnyObjectVirtualSchema;
                break;

            case USER:
                result = IntMappingType.UserVirtualSchema;
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends AnyTO> T newAnyTO() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = (T) new UserTO();
                break;

            case GROUP:
                result = (T) new GroupTO();
                break;

            case ANY_OBJECT:
                result = (T) new AnyObjectTO();
                break;

            default:
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Set<ExternalResource> getAllResources(final Any<?> any) {
        Set<ExternalResource> resources = new HashSet<>();

        if (any instanceof User) {
            resources.addAll(userDAO.findAllResources((User) any));
        } else if (any instanceof Group) {
            resources.addAll(((Group) any).getResources());
        } else if (any instanceof AnyObject) {
            resources.addAll(anyObjectDAO.findAllResources((AnyObject) any));
        }

        return resources;
    }

    @Transactional(readOnly = true)
    @Override
    public <S extends Schema> Set<S> getAllowedSchemas(final Any<?> any, final Class<S> reference) {
        Set<S> schemas = new HashSet<>();

        if (any instanceof User) {
            schemas.addAll(userDAO.findAllowedSchemas((User) any, reference));
        } else if (any instanceof Group) {
            schemas.addAll(groupDAO.findAllowedSchemas((Group) any, reference));
        } else if (any instanceof AnyObject) {
            schemas.addAll(anyObjectDAO.findAllowedSchemas((AnyObject) any, reference));
        }

        return schemas;
    }
}
