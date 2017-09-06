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
package org.apache.syncope.core.persistence.jpa.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

public abstract class AbstractAnySearchDAO extends AbstractDAO<Any<?>> implements AnySearchDAO {

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected DynRealmDAO dynRealmDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainSchemaDAO schemaDAO;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    protected SearchCond buildEffectiveCond(final SearchCond cond, final Set<String> dynRealmKeys) {
        List<SearchCond> effectiveConds = dynRealmKeys.stream().map(dynRealmKey -> {
            DynRealmCond dynRealmCond = new DynRealmCond();
            dynRealmCond.setDynRealm(dynRealmKey);
            return SearchCond.getLeafCond(dynRealmCond);
        }).collect(Collectors.toList());
        effectiveConds.add(cond);

        return SearchCond.getAndCond(effectiveConds);
    }

    protected abstract int doCount(Set<String> adminRealms, SearchCond cond, AnyTypeKind kind);

    @Override
    public int count(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        if (adminRealms == null || adminRealms.isEmpty()) {
            LOG.error("No realms provided");
            return 0;
        }

        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return 0;
        }

        return doCount(adminRealms, cond, kind);
    }

    @Override
    public <T extends Any<?>> List<T> search(final SearchCond cond, final AnyTypeKind kind) {
        return search(cond, Collections.<OrderByClause>emptyList(), kind);
    }

    @Override
    public <T extends Any<?>> List<T> search(
            final SearchCond cond, final List<OrderByClause> orderBy, final AnyTypeKind kind) {

        return search(SyncopeConstants.FULL_ADMIN_REALMS, cond, -1, -1, orderBy, kind);
    }

    protected abstract <T extends Any<?>> List<T> doSearch(
            Set<String> adminRealms,
            SearchCond searchCondition,
            int page,
            int itemsPerPage,
            List<OrderByClause> orderBy,
            AnyTypeKind kind);

    protected Pair<PlainSchema, PlainAttrValue> check(final AttributeCond cond, final AnyTypeKind kind) {
        AnyUtils attrUtils = anyUtilsFactory.getInstance(kind);

        PlainSchema schema = schemaDAO.find(cond.getSchema());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            throw new IllegalArgumentException();
        }

        PlainAttrValue attrValue = attrUtils.newPlainAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE
                    && cond.getType() != AttributeCond.Type.ILIKE
                    && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                schema.getValidator().validate(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
            throw new IllegalArgumentException();
        }

        return Pair.of(schema, attrValue);
    }

    protected Triple<PlainSchema, PlainAttrValue, AnyCond> check(final AnyCond cond, final AnyTypeKind kind) {
        AnyCond condClone = SerializationUtils.clone(cond);

        AnyUtils attrUtils = anyUtilsFactory.getInstance(kind);

        // Keeps track of difference between entity's getKey() and JPA @Id fields
        if ("key".equals(condClone.getSchema())) {
            condClone.setSchema("id");
        }

        Field anyField = ReflectionUtils.findField(attrUtils.anyClass(), condClone.getSchema());
        if (anyField == null) {
            LOG.warn("Ignoring invalid schema '{}'", condClone.getSchema());
            throw new IllegalArgumentException();
        }

        PlainSchema schema = new JPAPlainSchema();
        schema.setKey(anyField.getName());
        for (AttrSchemaType attrSchemaType : AttrSchemaType.values()) {
            if (anyField.getType().isAssignableFrom(attrSchemaType.getType())) {
                schema.setType(attrSchemaType);
            }
        }

        // Deal with any Integer fields logically mapping to boolean values
        boolean foundBooleanMin = false;
        boolean foundBooleanMax = false;
        if (Integer.class.equals(anyField.getType())) {
            for (Annotation annotation : anyField.getAnnotations()) {
                if (Min.class.equals(annotation.annotationType())) {
                    foundBooleanMin = ((Min) annotation).value() == 0;
                } else if (Max.class.equals(annotation.annotationType())) {
                    foundBooleanMax = ((Max) annotation).value() == 1;
                }
            }
        }
        if (foundBooleanMin && foundBooleanMax) {
            schema.setType(AttrSchemaType.Boolean);
        }

        // Deal with any fields representing relationships to other entities
        if (anyField.getType().getAnnotation(Entity.class) != null) {
            Method relMethod = null;
            try {
                relMethod = ClassUtils.getPublicMethod(anyField.getType(), "getKey", new Class<?>[0]);
            } catch (Exception e) {
                LOG.error("Could not find {}#getKey", anyField.getType(), e);
            }

            if (relMethod != null && String.class.isAssignableFrom(relMethod.getReturnType())) {
                condClone.setSchema(condClone.getSchema() + "_id");
                schema.setType(AttrSchemaType.String);
            }
        }

        PlainAttrValue attrValue = attrUtils.newPlainAttrValue();
        if (condClone.getType() != AttributeCond.Type.LIKE
                && condClone.getType() != AttributeCond.Type.ILIKE
                && condClone.getType() != AttributeCond.Type.ISNULL
                && condClone.getType() != AttributeCond.Type.ISNOTNULL) {

            try {
                schema.getValidator().validate(condClone.getExpression(), attrValue);
            } catch (ValidationException e) {
                LOG.error("Could not validate expression '" + condClone.getExpression() + "'", e);
                throw new IllegalArgumentException();
            }
        }

        return Triple.of(schema, attrValue, condClone);
    }

    protected String check(final MembershipCond cond) {
        String groupKey;
        if (SyncopeConstants.UUID_PATTERN.matcher(cond.getGroup()).matches()) {
            groupKey = cond.getGroup();
        } else {
            Group group = groupDAO.findByName(cond.getGroup());
            groupKey = group == null ? null : group.getKey();
        }
        if (groupKey == null) {
            LOG.error("Could not find group for '" + cond.getGroup() + "'");
            throw new IllegalArgumentException();
        }

        return groupKey;
    }

    protected String check(final RelationshipCond cond) {
        String rightAnyObjectKey;
        if (SyncopeConstants.UUID_PATTERN.matcher(cond.getAnyObject()).matches()) {
            rightAnyObjectKey = cond.getAnyObject();
        } else {
            AnyObject anyObject = anyObjectDAO.findByName(cond.getAnyObject());
            rightAnyObjectKey = anyObject == null ? null : anyObject.getKey();
        }
        if (rightAnyObjectKey == null) {
            LOG.error("Could not find any object for '" + cond.getAnyObject() + "'");
            throw new IllegalArgumentException();
        }

        return rightAnyObjectKey;
    }

    protected Realm check(final AssignableCond cond) {
        Realm realm = realmDAO.findByFullPath(cond.getRealmFullPath());
        if (realm == null) {
            LOG.error("Could not find realm for '" + cond.getRealmFullPath() + "'");
            throw new IllegalArgumentException();
        }

        return realm;
    }

    protected String check(final MemberCond cond) {
        String memberKey;
        if (SyncopeConstants.UUID_PATTERN.matcher(cond.getMember()).matches()) {
            memberKey = cond.getMember();
        } else {
            Any<?> member = userDAO.findByUsername(cond.getMember());
            if (member == null) {
                member = anyObjectDAO.findByName(cond.getMember());
            }
            memberKey = member == null ? null : member.getKey();
        }
        if (memberKey == null) {
            LOG.error("Could not find user or any object for '" + cond.getMember() + "'");
            throw new IllegalArgumentException();
        }

        return memberKey;
    }

    protected <T extends Any<?>> List<T> buildResult(final List<Object> raw, final AnyTypeKind kind) {
        List<T> result = new ArrayList<>();

        raw.stream().map(anyKey -> anyKey instanceof Object[]
                ? (String) ((Object[]) anyKey)[0]
                : ((String) anyKey)).
                forEachOrdered((actualKey) -> {
                    @SuppressWarnings("unchecked")
                    T any = kind == AnyTypeKind.USER
                            ? (T) userDAO.find(actualKey)
                            : kind == AnyTypeKind.GROUP
                                    ? (T) groupDAO.find(actualKey)
                                    : (T) anyObjectDAO.find(actualKey);
                    if (any == null) {
                        LOG.error("Could not find {} with id {}, even if returned by native query", kind, actualKey);
                    } else if (!result.contains(any)) {
                        result.add(any);
                    }
                });

        return result;
    }

    @Override
    public <T extends Any<?>> List<T> search(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        if (adminRealms == null || adminRealms.isEmpty()) {
            LOG.error("No realms provided");
            return Collections.<T>emptyList();
        }

        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return Collections.<T>emptyList();
        }

        List<OrderByClause> effectiveOrderBy;
        if (orderBy.isEmpty()) {
            OrderByClause keyClause = new OrderByClause();
            keyClause.setField("key");
            keyClause.setDirection(OrderByClause.Direction.ASC);
            effectiveOrderBy = Collections.singletonList(keyClause);
        } else {
            effectiveOrderBy = orderBy;
        }

        return doSearch(adminRealms, cond, page, itemsPerPage, effectiveOrderBy, kind);
    }

    @Override
    public <T extends Any<?>> boolean matches(final T any, final SearchCond cond) {
        return search(cond, any.getType().getKind()).contains(any);
    }
}
