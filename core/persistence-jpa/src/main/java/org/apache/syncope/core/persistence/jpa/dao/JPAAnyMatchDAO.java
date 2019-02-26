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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
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
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
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
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.springframework.beans.BeanUtils;

@Component
public class JPAAnyMatchDAO extends AbstractDAO<Any<?>> implements AnyMatchDAO {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

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
                if (cond.getAnyTypeCond() != null && AnyTypeKind.ANY_OBJECT == any.getType().getKind()) {
                    return matches(any, cond.getAnyTypeCond(), not);
                } else if (cond.getRelationshipTypeCond() != null && any instanceof GroupableRelatable) {
                    return matches((GroupableRelatable) any, cond.getRelationshipTypeCond(), not);
                } else if (cond.getRelationshipCond() != null && any instanceof GroupableRelatable) {
                    return matches((GroupableRelatable) any, cond.getRelationshipCond(), not);
                } else if (cond.getMembershipCond() != null && any instanceof GroupableRelatable) {
                    return matches((GroupableRelatable) any, cond.getMembershipCond(), not);
                } else if (cond.getAssignableCond() != null) {
                    return matches(any, cond.getAssignableCond(), not);
                } else if (cond.getRoleCond() != null && any instanceof User) {
                    return matches((User) any, cond.getRoleCond(), not);
                } else if (cond.getDynRealmCond() != null) {
                    return matches(any, cond.getDynRealmCond(), not);
                } else if (cond.getMemberCond() != null && any instanceof Group) {
                    return matches((Group) any, cond.getMemberCond(), not);
                } else if (cond.getResourceCond() != null) {
                    return matches(any, cond.getResourceCond(), not);
                } else if (cond.getAttributeCond() != null) {
                    return matches(any, cond.getAttributeCond(), not);
                } else if (cond.getAnyCond() != null) {
                    return matches(any, cond.getAnyCond(), not);
                }
                break;

            case AND:
                return matches(any, cond.getLeftSearchCond()) && matches(any, cond.getRightSearchCond());

            case OR:
                return matches(any, cond.getLeftSearchCond()) || matches(any, cond.getRightSearchCond());

            default:
        }

        return false;
    }

    private boolean matches(final Any<?> any, final AnyTypeCond cond, final boolean not) {
        boolean equals = any.getType().getKey().equals(cond.getAnyTypeKey());
        return not ? !equals : equals;
    }

    private boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final RelationshipTypeCond cond, final boolean not) {

        boolean found = IterableUtils.matchesAny(any.getRelationships(), new Predicate<Relationship<?, ?>>() {

            @Override
            public boolean evaluate(final Relationship<?, ?> rel) {
                return rel.getType().getKey().equals(cond.getRelationshipTypeKey());
            }
        });
        return not ? !found : found;
    }

    private boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final RelationshipCond cond, final boolean not) {

        String anyObject = cond.getAnyObject();
        if (!SyncopeConstants.UUID_PATTERN.matcher(cond.getAnyObject()).matches()) {
            anyObject = anyObjectDAO.findKey(anyObject);
        }

        boolean found = !any.getRelationships(anyObject).isEmpty();
        return not ? !found : found;
    }

    private boolean matches(
            final GroupableRelatable<?, ?, ?, ?, ?> any, final MembershipCond cond, final boolean not) {

        final String group = SyncopeConstants.UUID_PATTERN.matcher(cond.getGroup()).matches()
                ? cond.getGroup()
                : groupDAO.findKey(cond.getGroup());

        boolean found = any.getMembership(group) != null
                || IterableUtils.matchesAny((any instanceof User)
                        ? userDAO.findDynGroups(any.getKey())
                        : anyObjectDAO.findDynGroups(any.getKey()), new Predicate<Group>() {

                    @Override
                    public boolean evaluate(final Group item) {
                        return item.getKey().equals(group);
                    }
                });
        return not ? !found : found;
    }

    private boolean matches(final Any<?> any, final AssignableCond cond, final boolean not) {
        Realm realm = realmDAO.findByFullPath(cond.getRealmFullPath());
        boolean found = realm == null
                ? false
                : IterableUtils.matchesAny(
                        cond.isFromGroup() ? realmDAO.findDescendants(realm) : realmDAO.findAncestors(realm),
                        new Predicate<Realm>() {

                    @Override
                    public boolean evaluate(final Realm item) {
                        return item.equals(any.getRealm());
                    }
                });
        return not ? !found : found;
    }

    private boolean matches(final User user, final RoleCond cond, final boolean not) {
        boolean found = IterableUtils.matchesAny(userDAO.findAllRoles(user), new Predicate<Role>() {

            @Override
            public boolean evaluate(final Role role) {
                return role.getKey().equals(cond.getRole());
            }
        });
        return not ? !found : found;
    }

    private boolean matches(final Any<?> any, final DynRealmCond cond, final boolean not) {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
        boolean found = IterableUtils.matchesAny(anyUtils.dao().findDynRealms(any.getKey()), new Predicate<String>() {

            @Override
            public boolean evaluate(final String dynRealm) {
                return dynRealm.equals(cond.getDynRealm());
            }
        });
        return not ? !found : found;
    }

    private boolean matches(final Group group, final MemberCond cond, final boolean not) {
        boolean found = false;

        GroupableRelatable<?, ?, ?, ?, ?> any = userDAO.find(cond.getMember());
        if (any == null) {
            any = anyObjectDAO.find(cond.getMember());
            if (any != null) {
                found = CollectionUtils.collect(
                        groupDAO.findAMemberships(group), new Transformer<AMembership, String>() {

                    @Override
                    public String transform(final AMembership memb) {
                        return memb.getLeftEnd().getKey();
                    }
                }).contains(cond.getMember())
                        || groupDAO.findADynMembers(group).contains(cond.getMember());
            }
        } else {
            found = CollectionUtils.collect(
                    groupDAO.findUMemberships(group), new Transformer<UMembership, String>() {

                @Override
                public String transform(final UMembership memb) {
                    return memb.getLeftEnd().getKey();
                }
            }).contains(cond.getMember())
                    || groupDAO.findUDynMembers(group).contains(cond.getMember());
        }

        return not ? !found : found;
    }

    private boolean matches(final Any<?> any, final ResourceCond cond, final boolean not) {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
        boolean found = IterableUtils.matchesAny(anyUtils.getAllResources(any), new Predicate<ExternalResource>() {

            @Override
            public boolean evaluate(final ExternalResource resource) {
                return resource.getKey().equals(cond.getResourceKey());
            }
        });
        return not ? !found : found;
    }

    private boolean matches(
            final List<? extends PlainAttrValue> anyAttrValues,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttributeCond cond) {

        return IterableUtils.matchesAny(anyAttrValues, new Predicate<PlainAttrValue>() {

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public boolean evaluate(final PlainAttrValue item) {
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
                            return (cond.getType() == AttributeCond.Type.LIKE
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
            }
        });
    }

    private boolean matches(final Any<?> any, final AttributeCond cond, final boolean not) {
        PlainSchema schema = plainSchemaDAO.find(cond.getSchema());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return false;
        }

        PlainAttr<?> attr = any.getPlainAttr(cond.getSchema());

        boolean found;
        switch (cond.getType()) {
            case ISNULL:
                found = attr == null;
                break;

            case ISNOTNULL:
                found = attr != null;
                break;

            default:
                PlainAttrValue attrValue = anyUtilsFactory.getInstance(any).newPlainAttrValue();
                try {
                    if (cond.getType() != AttributeCond.Type.LIKE
                            && cond.getType() != AttributeCond.Type.ILIKE
                            && cond.getType() != AttributeCond.Type.ISNULL
                            && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                        schema.getValidator().validate(cond.getExpression(), attrValue);
                    }
                } catch (ValidationException e) {
                    LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
                    return false;
                }

                found = attr != null && matches(attr.getValues(), attrValue, schema, cond);
        }
        return not ? !found : found;
    }

    private boolean matches(final Any<?> any, final AnyCond cond, final boolean not) {
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
                if (cond.getType() != AttributeCond.Type.LIKE
                        && cond.getType() != AttributeCond.Type.ILIKE
                        && cond.getType() != AttributeCond.Type.ISNULL
                        && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                    try {
                        schema.getValidator().validate(cond.getExpression(), attrValue);
                    } catch (ValidationException e) {
                        LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
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
                } else if (anyAttrValue instanceof Date) {
                    anyAttrValues.get(0).setDateValue((Date) anyAttrValue);
                } else if (anyAttrValue instanceof byte[]) {
                    anyAttrValues.get(0).setBinaryValue((byte[]) anyAttrValue);
                }

                found = matches(anyAttrValues, attrValue, schema, cond);
        }
        return not ? !found : found;
    }
}
