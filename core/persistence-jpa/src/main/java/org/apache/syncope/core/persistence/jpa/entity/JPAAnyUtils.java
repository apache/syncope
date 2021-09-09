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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JPAAnyUtils implements AnyUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyUtils.class);

    protected static final Map<String, Field> USER_FIELDS = new HashMap<>();

    protected static final Map<String, Field> GROUP_FIELDS = new HashMap<>();

    protected static final Map<String, Field> ANY_OBJECT_FIELDS = new HashMap<>();

    static {
        initFieldNames(JPAUser.class, USER_FIELDS);
        initFieldNames(JPAGroup.class, GROUP_FIELDS);
        initFieldNames(JPAAnyObject.class, ANY_OBJECT_FIELDS);
    }

    protected static void initFieldNames(final Class<?> entityClass, final Map<String, Field> fields) {
        List<Class<?>> classes = ClassUtils.getAllSuperclasses(entityClass);
        classes.add(entityClass);
        classes.forEach(clazz -> {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        && !field.getName().startsWith("pc")
                        && !Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    fields.put(field.getName(), field);
                    if ("id".equals(field.getName())) {
                        fields.put("key", field);
                    }
                }
            }
        });
    }

    public static boolean matchesFieldName(final String candidate) {
        return USER_FIELDS.containsKey(candidate)
                || GROUP_FIELDS.containsKey(candidate)
                || ANY_OBJECT_FIELDS.containsKey(candidate);
    }

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final EntityFactory entityFactory;

    protected final AnyTypeKind anyTypeKind;

    protected final boolean linkedAccount;

    protected JPAAnyUtils(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final EntityFactory entityFactory,
            final AnyTypeKind anyTypeKind,
            final boolean linkedAccount) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.entityFactory = entityFactory;
        this.anyTypeKind = anyTypeKind;
        this.linkedAccount = linkedAccount;
    }

    @Override
    public AnyTypeKind anyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public <T extends Any<?>> Class<T> anyClass() {
        Class result;

        switch (anyTypeKind) {
            case GROUP:
                result = entityFactory.groupClass();
                break;

            case ANY_OBJECT:
                result = entityFactory.anyObjectClass();
                break;

            case USER:
            default:
                result = entityFactory.userClass();
        }

        return result;
    }

    @Override
    public Field getField(final String name) {
        Map<String, Field> fields;

        switch (anyTypeKind) {
            case GROUP:
                fields = GROUP_FIELDS;
                break;

            case ANY_OBJECT:
                fields = ANY_OBJECT_FIELDS;
                break;

            case USER:
            default:
                fields = USER_FIELDS;
                break;
        }

        return fields.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttr<?>> Class<T> plainAttrClass() {
        return (Class<T>) newPlainAttr().getClass();
    }

    @Override
    public <T extends PlainAttr<?>> T newPlainAttr() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = linkedAccount
                        ? (T) entityFactory.newEntity(LAPlainAttr.class)
                        : (T) entityFactory.newEntity(UPlainAttr.class);
                break;

            case GROUP:
                result = (T) entityFactory.newEntity(GPlainAttr.class);
                break;

            case ANY_OBJECT:
                result = (T) entityFactory.newEntity(APlainAttr.class);
                break;

            default:
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttrValue> Class<T> plainAttrValueClass() {
        return (Class<T>) newPlainAttrValue().getClass();
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrValue() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = linkedAccount
                        ? (T) entityFactory.newEntity(LAPlainAttrValue.class)
                        : (T) entityFactory.newEntity(UPlainAttrValue.class);
                break;

            case GROUP:
                result = (T) entityFactory.newEntity(GPlainAttrValue.class);
                break;

            case ANY_OBJECT:
                result = (T) entityFactory.newEntity(APlainAttrValue.class);
                break;

            default:
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttrValue> Class<T> plainAttrUniqueValueClass() {
        return (Class<T>) newPlainAttrUniqueValue().getClass();
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrUniqueValue() {
        T result = null;

        switch (anyTypeKind) {
            case USER:
                result = linkedAccount
                        ? (T) entityFactory.newEntity(LAPlainAttrUniqueValue.class)
                        : (T) entityFactory.newEntity(UPlainAttrUniqueValue.class);
                break;

            case GROUP:
                result = (T) entityFactory.newEntity(GPlainAttrUniqueValue.class);
                break;

            case ANY_OBJECT:
                result = (T) entityFactory.newEntity(APlainAttrUniqueValue.class);
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> T clonePlainAttrValue(final T src) {
        T dst;
        if (src instanceof PlainAttrUniqueValue) {
            dst = newPlainAttrUniqueValue();
        } else {
            dst = newPlainAttrValue();
        }

        dst.setBinaryValue(src.getBinaryValue());
        dst.setBooleanValue(src.getBooleanValue());
        dst.setDateValue(src.getDateValue());
        dst.setDoubleValue(src.getDoubleValue());
        dst.setLongValue(src.getLongValue());
        dst.setStringValue(src.getStringValue());

        return dst;
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

    @Override
    public <C extends AnyCR> C newAnyCR() {
        C result = null;

        switch (anyTypeKind) {
            case USER:
                result = (C) new UserCR();
                break;

            case GROUP:
                result = (C) new GroupCR();
                break;

            case ANY_OBJECT:
                result = (C) new AnyObjectCR();
                break;

            default:
        }

        return result;
    }

    @Override
    public <U extends AnyUR> U newAnyUR(final String key) {
        U result = null;

        switch (anyTypeKind) {
            case USER:
                result = (U) new UserUR();
                break;

            case GROUP:
                result = (U) new GroupUR();
                break;

            case ANY_OBJECT:
                result = (U) new AnyObjectUR();
                break;

            default:
        }

        if (result != null) {
            result.setKey(key);
        }

        return result;
    }

    @Override
    public <A extends Any<?>> AnyDAO<A> dao() {
        AnyDAO<A> result = null;

        switch (anyTypeKind) {
            case USER:
                result = (AnyDAO<A>) userDAO;
                break;

            case GROUP:
                result = (AnyDAO<A>) groupDAO;
                break;

            case ANY_OBJECT:
                result = (AnyDAO<A>) anyObjectDAO;
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

    @Transactional
    @Override
    public void addAttr(final String key, final PlainSchema schema, final String value) {
        Any any = dao().find(key);

        Set<AnyTypeClass> typeOwnClasses = new HashSet<>();
        typeOwnClasses.addAll(any.getType().getClasses());
        typeOwnClasses.addAll(any.getAuxClasses());
        if (!typeOwnClasses.stream().anyMatch(clazz -> clazz.getPlainSchemas().contains(schema))) {
            LOG.warn("Schema {} not allowed for {}, ignoring", schema, any);
            return;
        }

        PlainAttr<?> attr = (PlainAttr<?>) any.getPlainAttr(schema.getKey()).orElse(null);
        if (attr == null) {
            attr = newPlainAttr();
            attr.setSchema(schema);
            ((PlainAttr) attr).setOwner(any);
            any.add(attr);

            try {
                attr.add(value, this);
                dao().save(any);
            } catch (InvalidPlainAttrValueException e) {
                LOG.error("Invalid value for attribute {} and {}: {}", schema.getKey(), any, value, e);
            }
        } else {
            LOG.debug("{} has already {} set: {}", any, schema.getKey(), attr.getValuesAsStrings());
        }
    }
}
