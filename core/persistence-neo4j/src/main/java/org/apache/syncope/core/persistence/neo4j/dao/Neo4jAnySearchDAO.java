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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.text.TextStringBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
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
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AbstractAnySearchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DynRealmRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.GroupRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RoleRepoExt;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRelationshipType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.util.Streamable;

public class Neo4jAnySearchDAO extends AbstractAnySearchDAO {

    protected record AdminRealmsFilter(String filter, Set<String> dynRealmKeys, Set<String> groupOwners) {

    }

    protected record QueryInfo(
            TextStringBuilder query,
            Set<String> fields,
            Set<PlainSchema> plainSchemas,
            List<Pair<String, PlainSchema>> membershipAttrConds) {

    }

    protected static String setParameter(final Map<String, Object> parameters, final Object parameter) {
        String name = "param" + parameters.size();
        parameters.put(name, parameter);
        return name;
    }

    protected static void appendPlainAttrCond(
            final TextStringBuilder query, final PlainSchema schema, final String cond) {

        if (schema.isUniqueConstraint()) {
            query.append(schema.getKey()).append('.').append(key(schema.getType())).append(cond);
        } else {
            query.append("any(k IN ").append(schema.getKey()).
                    append(" WHERE k").append('.').append(key(schema.getType())).append(cond).
                    append(")");
        }
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr
                ? new StringBuilder().append('"').append(value).append('"').toString()
                : value;
    }

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    public Neo4jAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                plainSchemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator);
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    @Override
    protected boolean isPatternMatch(final String clause) {
        return clause.indexOf('*') != -1;
    }

    protected String buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final Map<String, Object> parameters) {

        if (realmKeys.isEmpty()) {
            return "(n)-[]-(:" + Neo4jRealm.NODE + ")";
        }

        return "(n)-[]-(r:" + Neo4jRealm.NODE + ") WHERE r.id IN $" + setParameter(parameters, realmKeys);
    }

    protected AdminRealmsFilter getAdminRealmsFilter(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final Map<String, Object> parameters) {

        Set<String> realmKeys = new HashSet<>();
        Set<String> dynRealmKeys = new HashSet<>();
        Set<String> groupOwners = new HashSet<>();

        if (recursive) {
            adminRealms.forEach(realmPath -> RealmUtils.parseGroupOwnerRealm(realmPath).ifPresentOrElse(
                    goRealm -> groupOwners.add(goRealm.getRight()),
                    () -> {
                        if (realmPath.startsWith("/")) {
                            Realm realm = realmSearchDAO.findByFullPath(realmPath).orElseThrow(() -> {
                                SyncopeClientException noRealm =
                                        SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                                noRealm.getElements().add("Invalid realm specified: " + realmPath);
                                return noRealm;
                            });

                            realmKeys.addAll(realmSearchDAO.findDescendants(realm.getFullPath(), base.getFullPath()));
                        } else {
                            dynRealmDAO.findById(realmPath).ifPresentOrElse(
                                    dynRealm -> dynRealmKeys.add(dynRealm.getKey()),
                                    () -> LOG.warn("Ignoring invalid dynamic realm {}", realmPath));
                        }
                    }));
            if (!dynRealmKeys.isEmpty()) {
                realmKeys.clear();
            }
        } else {
            if (adminRealms.stream().anyMatch(r -> r.startsWith(base.getFullPath()))) {
                realmKeys.add(base.getKey());
            }
        }

        return new AdminRealmsFilter(buildAdminRealmsFilter(realmKeys, parameters), dynRealmKeys, groupOwners);
    }

    protected String getQuery(
            final AnyTypeCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "(n)-[]-"
                + "(:" + Neo4jAnyType.NODE + " {id: $" + setParameter(parameters, cond.getAnyTypeKey()) + "}) ";
    }

    protected String getQuery(
            final AuxClassCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "(n)-[]-"
                + "(:" + Neo4jAnyTypeClass.NODE + " {id: $" + setParameter(parameters, cond.getAuxClass()) + "}) ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final RelationshipTypeCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String relTypeNode = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.NODE
                : Neo4jURelationship.NODE;

        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "EXISTS { MATCH (n)-[]-(r:" + relTypeNode + ")-[]-"
                + "(t:" + Neo4jRelationshipType.NODE
                + " {id: $ " + setParameter(parameters, cond.getRelationshipTypeKey()) + "}) } ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final RelationshipCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        Set<String> rightAnyObjects = check(cond);

        String relTypeNode = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.NODE
                : Neo4jURelationship.NODE;
        String destRelType = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.DEST_REL
                : Neo4jURelationship.DEST_REL;

        return "MATCH (n) "
                + "WHERE EXISTS { "
                + "MATCH(n)-[]-(:" + relTypeNode + ")-[:" + destRelType + "]-(anyObject:" + Neo4jAnyObject.NODE + ") "
                + "WHERE anyObject.id " + (not ? "NOT " : "") + "IN $" + setParameter(parameters, rightAnyObjects)
                + " } ";
    }

    protected String getQuery(
            final MembershipCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        cond.setGroup(cond.getGroup().replace("%", ".*"));
        List<String> groupKeys = check(cond);

        String param = setParameter(parameters, groupKeys);
        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jUMembership.NODE + ")-[]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jAMembership.NODE + ")-[]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } ";
    }

    protected String getQuery(
            final MemberCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        Set<String> memberKeys = check(cond);

        String param = setParameter(parameters, memberKeys);
        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jUMembership.NODE + ")-[]-"
                + "(m:" + Neo4jUser.NODE + ") WHERE m.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(m:" + Neo4jUser.NODE + ") WHERE m.id IN $" + param + " }  "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jAMembership.NODE + ")-[]-"
                + "(m:" + Neo4jAnyObject.NODE + ") WHERE m.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(m:" + Neo4jAnyObject.NODE + ") WHERE m.id IN $" + param + " } ";
    }

    protected String getQuery(
            final RoleCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String param = setParameter(parameters, cond.getRole());
        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "")
                + "(n)-[:" + Neo4jUser.ROLE_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jRole.NODE + " {id: $" + param + "}) "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + RoleRepoExt.DYN_ROLE_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jRole.NODE + " {id: $" + param + "}) } ";
    }

    protected String getQuery(
            final DynRealmCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return "MATCH (n) "
                + "WHERE " + (not ? "NOT " : "") + "(n)-"
                + "[:" + DynRealmRepoExt.DYN_REALM_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jDynRealm.NODE + " {id: $" + setParameter(parameters, cond.getDynRealm()) + "}) ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final ResourceCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String param = setParameter(parameters, cond.getResource());
        TextStringBuilder query = new TextStringBuilder("MATCH (n) ").
                append("WHERE ").
                append(not ? "NOT " : "").
                append("(n)-[]-(:").append(Neo4jExternalResource.NODE).append(" {id: $").append(param).append("}) ");

        if (kind == AnyTypeKind.USER || kind == AnyTypeKind.ANY_OBJECT) {
            String membershipTypeNode = kind == AnyTypeKind.ANY_OBJECT
                    ? Neo4jAMembership.NODE
                    : Neo4jUMembership.NODE;

            if (not) {
                query.append("AND NOT EXISTS { ");
            } else {
                query.append("OR EXISTS { ");
            }

            query.append("MATCH (n)-[]-(:").append(membershipTypeNode).append(")-[]-").
                    append("(g:").append(Neo4jGroup.NODE).append(") ").
                    append("WHERE ").
                    append("(g)-[]-(:").append(Neo4jExternalResource.NODE).append(" {id: $").append(param).append("})").
                    append(" } ");
        }

        return query.toString();
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            if (schema.isUniqueConstraint()) {
                fillAttrQuery(query, attrValue, schema, cond, false, parameters);
                query.replaceFirst("WHERE", "WHERE NOT(");
                query.append(')');
            } else {
                fillAttrQuery(query, attrValue, schema, cond, false, parameters);
                query.replaceAll("any(", schema.getKey() + " IS NULL OR none(");
            }
            return;
        }

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(cond::getExpression);

        boolean isStr = true;
        boolean lower = false;
        if (schema.getType().isStringClass()) {
            lower = (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
        } else if (schema.getType() != AttrSchemaType.Date) {
            lower = false;
            try {
                switch (schema.getType()) {
                    case Long ->
                        Long.valueOf(value);

                    case Double ->
                        Double.valueOf(value);

                    case Boolean -> {
                        if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                            throw new IllegalArgumentException();
                        }
                    }

                    default -> {
                    }
                }

                isStr = false;
            } catch (Exception nfe) {
                // ignore
            }
        }

        query.append("WHERE ");

        switch (cond.getType()) {
            case ISNULL -> {
            }

            case ISNOTNULL ->
                query.append(schema.getKey()).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"" + (lower ? "(?i)" : "")
                            + AnyRepoExt.escapeForLikeRegex(value).replace("%", ".*") + '"');
                } else {
                    query.append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ -> {
                if (StringUtils.containsAny(value, AnyRepoExt.REGEX_CHARS) || lower) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"^" + (lower ? "(?i)" : "")
                            + AnyRepoExt.escapeForLikeRegex(value).replace("%", ".*") + "$\"");
                } else {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " = " + escapeIfString(value, isStr));
                }
            }

            case GE ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " >= " + escapeIfString(value, isStr));

            case GT ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " > " + escapeIfString(value, isStr));

            case LE ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " <= " + escapeIfString(value, isStr));

            case LT ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " < " + escapeIfString(value, isStr));

            default -> {
            }
        }
        // shouldn't occour: processed before
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            query.append("NOT (");
            fillAttrQuery(query, attrValue, schema, cond, false, parameters);
            query.append(')');
            return;
        }
        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }

        boolean lower = schema.getType().isStringClass()
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

        String property = "n." + cond.getSchema();
        if (lower) {
            property = "toLower (" + property + ')';
        }

        switch (cond.getType()) {

            case ISNULL ->
                query.append(property).append(" IS NULL");

            case ISNOTNULL ->
                query.append(property).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    query.append(property).append(" =~ ");
                    if (lower) {
                        query.append("toLower($").
                                append(setParameter(parameters, cond.getExpression().replace("%", ".*"))).
                                append(')');
                    } else {
                        query.append('$').append(setParameter(parameters, cond.getExpression().replace("%", ".*")));
                    }
                } else {
                    query.append(' ').append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ -> {
                query.append(property).append('=');

                if (lower) {
                    query.append("toLower($").append(setParameter(parameters, attrValue.getValue())).append(')');
                } else {
                    query.append('$').append(setParameter(parameters, attrValue.getValue()));
                }
            }

            case GE -> {
                query.append(property);
                if (not) {
                    query.append('<');
                } else {
                    query.append(">=");
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case GT -> {
                query.append(property);
                if (not) {
                    query.append("<=");
                } else {
                    query.append('>');
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case LE -> {
                query.append(property);
                if (not) {
                    query.append('>');
                } else {
                    query.append("<=");
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case LT -> {
                query.append(property);
                if (not) {
                    query.append(">=");
                } else {
                    query.append('<');
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            default -> {
            }
        }
    }

    protected Pair<String, String> getQuery(
            final AnyTypeKind kind,
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())) {
            if (!SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {
                Realm realm = realmSearchDAO.findByFullPath(cond.getExpression()).
                        orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Realm full path: " + cond.getExpression()));
                cond.setExpression(realm.getKey());
            }

            return Pair.of(
                    "MATCH (n)-[]-"
                    + "(:" + Neo4jRealm.NODE + " {id: $" + setParameter(parameters, cond.getExpression()) + "}) ",
                    null);
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, kind);

        if (ArrayUtils.contains(
                RELATIONSHIP_FIELDS,
                StringUtils.substringBefore(checked.getRight().getSchema(), "_id"))) {

            String field = StringUtils.substringBefore(checked.getRight().getSchema(), "_id");
            switch (field) {
                case "userOwner" -> {
                    return Pair.of(
                            "MATCH (n)-[:" + Neo4jGroup.USER_OWNER_REL + "]-"
                            + "(:" + Neo4jUser.NODE + " "
                            + "{id: $" + setParameter(parameters, cond.getExpression()) + "})",
                            null);
                }

                case "groupOwner" -> {
                    return Pair.of(
                            "MATCH (n)-[:" + Neo4jGroup.GROUP_OWNER_REL + "]-"
                            + "(:" + Neo4jGroup.NODE + " "
                            + "{id: $" + setParameter(parameters, cond.getExpression()) + "})",
                            null);
                }

                default ->
                    throw new IllegalArgumentException("Unsupported relationship: " + field);
            }
        }

        TextStringBuilder query = new TextStringBuilder("MATCH (n) WHERE ");

        fillAttrQuery(query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters);

        return Pair.of(query.toString(), checked.getRight().getSchema());
    }

    protected Pair<String, PlainSchema> getQuery(
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        Pair<PlainSchema, PlainAttrValue> checked = check(cond);

        TextStringBuilder query = new TextStringBuilder("MATCH (n) ");
        switch (cond.getType()) {
            case ISNOTNULL ->
                query.append("WHERE n.`plainAttrs.").append(checked.getLeft().getKey()).append("` IS NOT NULL");

            case ISNULL ->
                query.append("WHERE n.`plainAttrs.").append(checked.getLeft().getKey()).append("` IS NULL");

            default ->
                fillAttrQuery(query, checked.getRight(), checked.getLeft(), cond, not, parameters);
        }

        return Pair.of(query.toString(), checked.getLeft());
    }

    protected void getQueryForCustomConds(
            final AnyTypeKind kind,
            final SearchCond cond,
            final Map<String, Object> parameters,
            final boolean not,
            final TextStringBuilder query) {

        // do nothing by default, leave it open for subclasses
    }

    protected void queryOp(
            final TextStringBuilder query,
            final String op,
            final QueryInfo leftInfo,
            final QueryInfo rightInfo) {

        query.append("WHERE EXISTS { ").
                append(StringUtils.prependIfMissing(leftInfo.query().toString(), "MATCH (n) ")).
                append(" } ").
                append(op).append(" EXISTS { ").
                append(StringUtils.prependIfMissing(rightInfo.query().toString(), "MATCH (n) ")).
                append(" }");
    }

    protected QueryInfo getQuery(final AnyTypeKind kind, final SearchCond cond, final Map<String, Object> parameters) {
        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        TextStringBuilder query = new TextStringBuilder();
        Set<String> involvedFields = new HashSet<>();
        Set<PlainSchema> involvedPlainSchemas = new HashSet<>();
        List<Pair<String, PlainSchema>> membershipAttrConds = new ArrayList<>();

        switch (cond.getType()) {
            case LEAF, NOT_LEAF -> {
                cond.asLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == kind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(AuxClassCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(RelationshipTypeCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != kind).
                        ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

                cond.asLeaf(RelationshipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != kind).
                        ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

                cond.asLeaf(MembershipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != kind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(MemberCond.class).
                        filter(leaf -> AnyTypeKind.GROUP == kind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(RoleCond.class).
                        filter(leaf -> AnyTypeKind.USER == kind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(DynRealmCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

                cond.asLeaf(ResourceCond.class).
                        ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

                cond.asLeaf(AnyCond.class).ifPresentOrElse(
                        anyCond -> {
                            Pair<String, String> anyCondResult = getQuery(kind, anyCond, not, parameters);
                            query.append(anyCondResult.getLeft());
                            Optional.ofNullable(anyCondResult.getRight()).ifPresent(involvedFields::add);
                        },
                        () -> cond.asLeaf(AttrCond.class).ifPresent(leaf -> {
                            Pair<String, PlainSchema> attrCondResult = getQuery(leaf, not, parameters);
                            query.append(attrCondResult.getLeft());
                            involvedPlainSchemas.add(attrCondResult.getRight());
                            if (kind != AnyTypeKind.GROUP
                                    && !not
                                    && leaf.getType() != AttrCond.Type.ISNULL
                                    && leaf.getType() != AttrCond.Type.ISNOTNULL) {

                                membershipAttrConds.add(attrCondResult);
                            }
                        }));

                // allow for additional search conditions
                getQueryForCustomConds(kind, cond, parameters, not, query);
            }
            case AND -> {
                QueryInfo leftAndInfo = getQuery(kind, cond.getLeft(), parameters);
                involvedFields.addAll(leftAndInfo.fields());
                involvedPlainSchemas.addAll(leftAndInfo.plainSchemas());
                membershipAttrConds.addAll(leftAndInfo.membershipAttrConds());

                QueryInfo rigthAndInfo = getQuery(kind, cond.getRight(), parameters);
                involvedFields.addAll(rigthAndInfo.fields());
                involvedPlainSchemas.addAll(rigthAndInfo.plainSchemas());
                membershipAttrConds.addAll(rigthAndInfo.membershipAttrConds());

                queryOp(query, "AND", leftAndInfo, rigthAndInfo);
            }

            case OR -> {
                QueryInfo leftOrInfo = getQuery(kind, cond.getLeft(), parameters);
                involvedFields.addAll(leftOrInfo.fields());
                involvedPlainSchemas.addAll(leftOrInfo.plainSchemas());
                membershipAttrConds.addAll(leftOrInfo.membershipAttrConds());

                QueryInfo rigthOrInfo = getQuery(kind, cond.getRight(), parameters);
                involvedFields.addAll(rigthOrInfo.fields());
                involvedPlainSchemas.addAll(rigthOrInfo.plainSchemas());
                membershipAttrConds.addAll(rigthOrInfo.membershipAttrConds());

                queryOp(query, "OR", leftOrInfo, rigthOrInfo);
            }

            default -> {
            }
        }

        return new QueryInfo(query, involvedFields, involvedPlainSchemas, membershipAttrConds);
    }

    protected void wrapQuery(
            final QueryInfo queryInfo,
            final Streamable<Order> orderBy,
            final AnyTypeKind kind,
            final String adminRealmsFilter) {

        TextStringBuilder match = new TextStringBuilder("MATCH (n:").append(AnyRepoExt.node(kind)).append(") ").
                append("WITH n.id AS id");

        // take fields into account
        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);
        queryInfo.fields().remove("id");
        Stream.concat(
                queryInfo.fields().stream(),
                orderBy.stream().filter(clause -> !"id".equals(clause.getProperty())
                && anyUtils.getField(clause.getProperty()).isPresent()).map(Order::getProperty)).
                distinct().forEach(field -> match.append(", n.").append(field).append(" AS ").append(field));

        // take plain schemas into account
        Stream.concat(
                queryInfo.plainSchemas().stream(),
                orderBy.stream().map(clause -> plainSchemaDAO.findById(clause.getProperty())).
                        flatMap(Optional::stream)).distinct().forEach(schema -> {

            match.append(", apoc.convert.getJsonProperty(n, 'plainAttrs.").append(schema.getKey());
            if (schema.isUniqueConstraint()) {
                match.append("', '$.uniqueValue')");
            } else {
                match.append("', '$.values')");
            }
            match.append(" AS ").append(schema.getKey());
        });

        TextStringBuilder query = queryInfo.query();

        // take realms into account
        if (query.startsWith("MATCH (n)")) {
            query.replaceFirst("MATCH (n)", match + " WHERE (EXISTS { MATCH (n)");
            query.append("} ");
        } else {
            query.replaceFirst("WHERE EXISTS", "WHERE (EXISTS");
            query.insert(0, match.append(' '));
        }
        query.append(") AND EXISTS { ").append(adminRealmsFilter).append(" } ");
    }

    protected void membershipAttrConds(
            final TextStringBuilder query,
            final QueryInfo queryInfo,
            final List<String> orderBy,
            final AnyTypeKind kind) {

        if (kind == AnyTypeKind.GROUP) {
            return;
        }
        if (queryInfo.membershipAttrConds().isEmpty()) {
            return;
        }

        Set<String> orderByItems = orderBy.stream().
                map(clause -> StringUtils.substringBefore(clause, " ")).
                collect(Collectors.toSet());

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);
        Set<String> fields = Stream.concat(
                queryInfo.fields().stream().filter(f -> !"id".equals(f)),
                orderByItems.stream().filter(item -> !"id".equals(item) && anyUtils.getField(item).isPresent())).
                collect(Collectors.toSet());

        Set<PlainSchema> plainSchemas = Stream.concat(
                queryInfo.membershipAttrConds().stream().map(Pair::getRight),
                orderByItems.stream().map(plainSchemaDAO::findById).flatMap(Optional::stream)).
                collect(Collectors.toSet());

        // call
        query.insert(0, "CALL () { ");

        // return
        TextStringBuilder returnStmt = new TextStringBuilder("RETURN id");

        fields.forEach(f -> returnStmt.append(", ").append(f));

        plainSchemas.forEach(schema -> returnStmt.append(", ").append(schema.getKey()));

        query.append(returnStmt);

        // union
        query.append(" UNION ").
                append("MATCH (n:").append(AnyRepoExt.membNode(kind)).
                append(")-[]-(m:").append(AnyRepoExt.node(kind) + ") ").
                append("WITH m.id AS id ");

        fields.forEach(f -> query.append(", m.").append(f).append(" AS ").append(f));

        plainSchemas.forEach(schema -> {
            query.append(", apoc.convert.getJsonProperty(n, 'plainAttrs.").append(schema.getKey());
            if (schema.isUniqueConstraint()) {
                query.append("', '$.uniqueValue')");
            } else {
                query.append("', '$.values')");
            }
            query.append(" AS ").append(schema.getKey());
        });

        query.append(" WHERE ");

        query.append(queryInfo.membershipAttrConds().stream().
                map(mac -> "(EXISTS { " + mac.getLeft() + "} )").
                collect(Collectors.joining(" AND ")));

        query.append(" AND EXISTS { (m)-[]-(r:Realm) WHERE r.id IN $param0 } ").
                append(returnStmt).
                append(" } ");
    }

    @Override
    protected long doCount(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        Map<String, Object> parameters = new HashMap<>();

        AdminRealmsFilter filter = getAdminRealmsFilter(base, recursive, adminRealms, parameters);

        // 1. get the query string from the search condition
        QueryInfo queryInfo = getQuery(
                kind, buildEffectiveCond(cond, filter.dynRealmKeys(), filter.groupOwners(), kind), parameters);

        // 2. wrap query
        wrapQuery(queryInfo, Streamable.empty(), kind, filter.filter());
        TextStringBuilder query = queryInfo.query();

        // 3. include membership plain attr queries
        membershipAttrConds(query, queryInfo, List.of(), kind);

        // 4. prepare the count query
        query.append("RETURN COUNT(id)");

        return neo4jTemplate.count(query.toString(), parameters);
    }

    protected List<String> parseOrderBy(
            final AnyTypeKind kind,
            final Streamable<Sort.Order> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);

        List<String> clauses = new ArrayList<>();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            if (anyUtils.getField(clause.getProperty()).isPresent()) {
                clauses.add(clause.getProperty() + " " + clause.getDirection().name());
            } else {
                plainSchemaDAO.findById(clause.getProperty()).ifPresent(schema -> {
                    if (schema.isUniqueConstraint()) {
                        orderByUniquePlainSchemas.add(schema.getKey());
                    } else {
                        orderByNonUniquePlainSchemas.add(schema.getKey());
                    }
                    if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                        SyncopeClientException invalidSearch =
                                SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                        invalidSearch.getElements().add("Order by more than one attribute is not allowed; "
                                + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas));
                        throw invalidSearch;
                    }

                    clauses.add(schema.getKey() + " " + clause.getDirection().name());
                });
            }
        });

        return clauses;
    }

    @Override
    protected <T extends Any> List<T> doSearch(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        Map<String, Object> parameters = new HashMap<>();

        AdminRealmsFilter filter = getAdminRealmsFilter(base, recursive, adminRealms, parameters);

        // 1. get the query string from the search condition
        QueryInfo queryInfo = getQuery(
                kind, buildEffectiveCond(cond, filter.dynRealmKeys(), filter.groupOwners(), kind), parameters);

        // 2. wrap query
        wrapQuery(queryInfo, pageable.getSort(), kind, filter.filter());
        TextStringBuilder query = queryInfo.query();

        List<String> orderBy = parseOrderBy(kind, pageable.getSort());
        String orderByStmt = String.join(", ", orderBy);

        // 3. include membership plain attr queries
        membershipAttrConds(query, queryInfo, orderBy, kind);

        // 4. prepare the search query
        query.append("RETURN id ").
                append("ORDER BY ").append(orderByStmt);

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        LOG.debug("Query with auth and order by statements: {}, parameters: {}", query, parameters);

        // 5. Prepare the result (avoiding duplicates)
        return buildResult(neo4jClient.query(query.toString()).bindAll(parameters).fetch().all().stream().
                map(found -> found.get("id")).toList(), kind);
    }
}
