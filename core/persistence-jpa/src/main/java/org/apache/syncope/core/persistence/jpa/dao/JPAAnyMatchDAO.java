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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.GroupableRelatable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.springframework.beans.BeanUtils;

public class JPAAnyMatchDAO extends AbstractDAO<Any<?>> implements AnyMatchDAO {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final RealmDAO realmDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final AnyUtilsFactory anyUtilsFactory;

    public JPAAnyMatchDAO(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final AnyUtilsFactory anyUtilsFactory) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.realmDAO = realmDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    /**
     * Verify if any matches the given search condition.
     *
     * @param any to be checked
     * @param cond to be verified
     * @param <T> any
     * @return true if any matches cond
     */
    @Transactional(readOnly = true)
    @Override
    public <T extends Any<?>> boolean matches(final T any, final SearchCond cond) {
        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;
        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                Boolean match = cond.getLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == any.getType().getKind()).
                        map(leaf -> matches(any, leaf, not)).
                        orElse(null);

                if (match == null) {
                    match = cond.getLeaf(RelationshipTypeCond.class).
                            filter(leaf -> any instanceof GroupableRelatable).
                            map(leaf -> matches((GroupableRelatable) any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(RelationshipCond.class).
                            filter(leaf -> any instanceof GroupableRelatable).
                            map(leaf -> matches((GroupableRelatable) any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(MembershipCond.class).
                            filter(leaf -> any instanceof GroupableRelatable).
                            map(leaf -> matches((GroupableRelatable) any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(AssignableCond.class).
                            map(leaf -> matches(any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(RoleCond.class).
                            filter(leaf -> any instanceof User).
                            map(leaf -> matches((User) any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(DynRealmCond.class).
                            map(leaf -> matches(any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(MemberCond.class).
                            filter(leaf -> any instanceof Group).
                            map(leaf -> matches((Group) any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    match = cond.getLeaf(ResourceCond.class).
                            map(leaf -> matches(any, leaf, not)).
                            orElse(null);
                }

                if (match == null) {
                    Optional<AnyCond> anyCond = cond.getLeaf(AnyCond.class);
                    match = anyCond.map(value -> matches(any, value, not)).orElseGet(() -> cond.getLeaf(AttrCond.class).
                            map(leaf -> matches(any, leaf, not)).
                            orElse(null));
                }

                if (match == null) {
                    match = cond.getLeaf(AttrCond.class).
                            map(leaf -> matches(any, leaf, not)).
                            orElse(null);
                }

                return BooleanUtils.toBoolean(match);

            case AND:
                return matches(any, cond.getLeft()) && matches(any, cond.getRight());

            case OR:
                return matches(any, cond.getLeft()) || matches(any, cond.getRight());

            default:
        }

        return false;
    }

    protected static boolean matches(final Any<?> any, final AnyTypeCond cond, final boolean not) {
        boolean equals = any.getType().getKey().equals(cond.getAnyTypeKey());
        return not ? !equals : equals;
    }

    protected static boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final RelationshipTypeCond cond, final boolean not) {

        boolean found = any.getRelationships().stream().
                anyMatch(rel -> rel.getType().getKey().equals(cond.getRelationshipTypeKey()));
        return not ? !found : found;
    }

    protected boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final RelationshipCond cond, final boolean not) {

        String anyObject = cond.getAnyObject();
        if (!SyncopeConstants.UUID_PATTERN.matcher(cond.getAnyObject()).matches()) {
            anyObject = anyObjectDAO.findKey(anyObject);
        }

        boolean found = !any.getRelationships(anyObject).isEmpty();
        return not ? !found : found;
    }

    protected boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final MembershipCond cond, final boolean not) {

        final String group = SyncopeConstants.UUID_PATTERN.matcher(cond.getGroup()).matches()
                ? cond.getGroup()
                : groupDAO.findKey(cond.getGroup());

        boolean found = any.getMembership(group).isPresent()
                || (any instanceof User
                        ? userDAO.findDynGroups(any.getKey())
                        : anyObjectDAO.findDynGroups(any.getKey())).stream().
                        anyMatch(item -> item.getKey().equals(group));
        return not ? !found : found;
    }

    protected boolean matches(final Any<?> any, final AssignableCond cond, final boolean not) {
        Realm realm = realmDAO.findByFullPath(cond.getRealmFullPath());
        boolean found = Optional.ofNullable(realm)
                .filter(realm1 -> (cond.isFromGroup() ? realmDAO.findDescendants(realm1) : realmDAO.
                findAncestors(realm1)).
                stream().anyMatch(item -> item.equals(any.getRealm()))).isPresent();
        return not ? !found : found;
    }

    protected boolean matches(final User user, final RoleCond cond, final boolean not) {
        boolean found = userDAO.findAllRoles(user).stream().anyMatch(role -> role.getKey().equals(cond.getRole()));
        return not ? !found : found;
    }

    protected boolean matches(final Any<?> any, final DynRealmCond cond, final boolean not) {
        boolean found = anyUtilsFactory.getInstance(any).dao().findDynRealms(any.getKey()).stream().
                anyMatch(dynRealm -> dynRealm.equals(cond.getDynRealm()));
        return not ? !found : found;
    }

    protected boolean matches(final Group group, final MemberCond cond, final boolean not) {
        boolean found = false;

        GroupableRelatable<?, ?, ?, ?, ?> any = userDAO.find(cond.getMember());
        if (any == null) {
            any = anyObjectDAO.find(cond.getMember());
            if (any != null) {
                found = groupDAO.findAMemberships(group).stream().
                        anyMatch(memb -> memb.getLeftEnd().getKey().equals(cond.getMember()))
                        || groupDAO.findADynMembers(group).contains(cond.getMember());
            }
        } else {
            found = groupDAO.findUMemberships(group).stream().
                    anyMatch(memb -> memb.getLeftEnd().getKey().equals(cond.getMember()))
                    || groupDAO.findUDynMembers(group).contains(cond.getMember());
        }

        return not ? !found : found;
    }

    protected boolean matches(final Any<?> any, final ResourceCond cond, final boolean not) {
        boolean found = anyUtilsFactory.getInstance(any).getAllResources(any).stream().
                anyMatch(resource -> resource.getKey().equals(cond.getResourceKey()));
        return not ? !found : found;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static boolean matches(
            final List<? extends PlainAttrValue> anyAttrValues,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond) {

        return anyAttrValues.stream().anyMatch(item -> {
            switch (cond.getType()) {
                case EQ:
                    return attrValue.getValue().equals(item.getValue());

                case IEQ:
                    if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                        return attrValue.getStringValue().equalsIgnoreCase(item.getStringValue());
                    } else {
                        LOG.error("IEQ is only compatible with string or enum schemas");
                        return false;
                    }

                case LIKE:
                case ILIKE:
                    if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                        StringBuilder output = new StringBuilder();
                        for (char c : cond.getExpression().toLowerCase().toCharArray()) {
                            if (c == '%') {
                                output.append(".*");
                            } else if (Character.isLetter(c)) {
                                output.append('[').
                                        append(c).
                                        append(Character.toUpperCase(c)).
                                        append(']');
                            } else {
                                output.append(c);
                            }
                        }
                        return (cond.getType() == AttrCond.Type.LIKE
                                ? Pattern.compile(output.toString())
                                : Pattern.compile(output.toString(), Pattern.CASE_INSENSITIVE)).
                                matcher(item.getStringValue()).matches();
                    } else {
                        LOG.error("LIKE is only compatible with string or enum schemas");
                        return false;
                    }

                case GT:
                    return item.<Comparable>getValue().compareTo(attrValue.getValue()) > 0;

                case GE:
                    return item.<Comparable>getValue().compareTo(attrValue.getValue()) >= 0;

                case LT:
                    return item.<Comparable>getValue().compareTo(attrValue.getValue()) < 0;

                case LE:
                    return item.<Comparable>getValue().compareTo(attrValue.getValue()) <= 0;

                default:
                    return false;
            }
        });
    }

    protected boolean matches(final Any<?> any, final AttrCond cond, final boolean not) {
        PlainSchema schema = plainSchemaDAO.find(cond.getSchema());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return false;
        }

        @SuppressWarnings("unchecked")
        Optional<PlainAttr<?>> attr = (Optional<PlainAttr<?>>) any.getPlainAttr(cond.getSchema());

        boolean found;
        switch (cond.getType()) {
            case ISNULL:
                found = attr.isEmpty();
                break;

            case ISNOTNULL:
                found = attr.isPresent();
                break;

            default:
                PlainAttrValue attrValue = anyUtilsFactory.getInstance(any).newPlainAttrValue();
                try {
                    if (cond.getType() != AttrCond.Type.LIKE
                            && cond.getType() != AttrCond.Type.ILIKE
                            && cond.getType() != AttrCond.Type.ISNULL
                            && cond.getType() != AttrCond.Type.ISNOTNULL) {

                        ((JPAPlainSchema) schema).validator().validate(cond.getExpression(), attrValue);
                    }
                } catch (ValidationException e) {
                    LOG.error("Could not validate expression '" + cond.getExpression() + '\'', e);
                    return false;
                }

                found = attr.isPresent() && matches(attr.get().getValues(), attrValue, schema, cond);
        }
        return not ? !found : found;
    }

    protected boolean matches(final Any<?> any, final AnyCond cond, final boolean not) {
        // Keeps track of difference between entity's getKey() and JPA @Id fields
        if ("key".equals(cond.getSchema())) {
            cond.setSchema("id");
        }

        PropertyDescriptor pd;
        Object anyAttrValue;
        try {
            pd = BeanUtils.getPropertyDescriptor(any.getClass(), cond.getSchema());
            if (pd == null) {
                LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
                return false;
            }

            anyAttrValue = pd.getReadMethod().invoke(any);
        } catch (Exception e) {
            LOG.error("While accessing {}.{}", any, cond.getSchema(), e);
            return false;
        }

        boolean found;
        switch (cond.getType()) {
            case ISNULL:
                found = anyAttrValue == null;
                break;

            case ISNOTNULL:
                found = anyAttrValue != null;
                break;

            default:
                PlainSchema schema = new JPAPlainSchema();
                schema.setKey(pd.getName());
                for (AttrSchemaType attrSchemaType : AttrSchemaType.values()) {
                    if (pd.getPropertyType().isAssignableFrom(attrSchemaType.getType())) {
                        schema.setType(attrSchemaType);
                    }
                }

                // Deal with any Integer fields logically mapping to boolean values
                boolean foundBooleanMin = false;
                boolean foundBooleanMax = false;
                if (Integer.class.equals(pd.getPropertyType())) {
                    for (Annotation annotation : pd.getPropertyType().getAnnotations()) {
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
                if (pd.getPropertyType().getAnnotation(Entity.class) != null) {
                    Method relMethod = null;
                    try {
                        relMethod = ClassUtils.getPublicMethod(pd.getPropertyType(), "getKey", new Class<?>[0]);
                    } catch (Exception e) {
                        LOG.error("Could not find {}#getKey", pd.getPropertyType(), e);
                    }

                    if (relMethod != null && String.class.isAssignableFrom(relMethod.getReturnType())) {
                        cond.setSchema(cond.getSchema() + "_id");
                        schema.setType(AttrSchemaType.String);
                    }
                }

                AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

                PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
                if (cond.getType() != AttrCond.Type.LIKE
                        && cond.getType() != AttrCond.Type.ILIKE
                        && cond.getType() != AttrCond.Type.ISNULL
                        && cond.getType() != AttrCond.Type.ISNOTNULL) {

                    try {
                        ((JPAPlainSchema) schema).validator().validate(cond.getExpression(), attrValue);
                    } catch (ValidationException e) {
                        LOG.error("Could not validate expression '" + cond.getExpression() + '\'', e);
                        return false;
                    }
                }

                List<PlainAttrValue> anyAttrValues = new ArrayList<>();
                anyAttrValues.add(anyUtils.newPlainAttrValue());
                if (anyAttrValue instanceof String) {
                    anyAttrValues.get(0).setStringValue((String) anyAttrValue);
                } else if (anyAttrValue instanceof Long) {
                    anyAttrValues.get(0).setLongValue((Long) anyAttrValue);
                } else if (anyAttrValue instanceof Double) {
                    anyAttrValues.get(0).setDoubleValue((Double) anyAttrValue);
                } else if (anyAttrValue instanceof Boolean) {
                    anyAttrValues.get(0).setBooleanValue((Boolean) anyAttrValue);
                } else if (anyAttrValue instanceof OffsetDateTime) {
                    anyAttrValues.get(0).setDateValue((OffsetDateTime) anyAttrValue);
                } else if (anyAttrValue instanceof byte[]) {
                    anyAttrValues.get(0).setBinaryValue((byte[]) anyAttrValue);
                }

                found = matches(anyAttrValues, attrValue, schema, cond);
        }
        return not ? !found : found;
    }
}
