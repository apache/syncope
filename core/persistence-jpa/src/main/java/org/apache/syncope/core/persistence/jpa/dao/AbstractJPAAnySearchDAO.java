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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
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
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Search engine implementation for users, groups and any objects, based on self-updating SQL views.
 */
abstract class AbstractJPAAnySearchDAO extends AbstractAnySearchDAO {

    protected static final String SELECT_COLS_FROM_VIEW =
            "any_id,creationContext,creationDate,creator,lastChangeContext,"
            + "lastChangeDate,lastModifier,status,changePwdDate,cipherAlgorithm,failedLogins,"
            + "lastLoginDate,mustChangePassword,suspended,username";

    private static final Map<String, Boolean> IS_ORACLE = new ConcurrentHashMap<>();

    protected static int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected static void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Boolean aBoolean) {
                query.setParameter(i + 1, aBoolean ? 1 : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    protected static Supplier<SyncopeClientException> syncopeClientException(final String message) {
        return () -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(message);
            return sce;
        };
    }

    protected final EntityManagerFactory entityManagerFactory;

    protected final EntityManager entityManager;

    protected AbstractJPAAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager) {

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
        this.entityManagerFactory = entityManagerFactory;
        this.entityManager = entityManager;
    }

    protected boolean isOracle() {
        return IS_ORACLE.computeIfAbsent(
                AuthContextUtils.getDomain(),
                k -> {
                    OpenJPAEntityManagerFactorySPI emfspi = entityManagerFactory.unwrap(
                            OpenJPAEntityManagerFactorySPI.class);
                    return ((MappingRepository) emfspi.getConfiguration().
                            getMetaDataRepositoryInstance()).getDBDictionary() instanceof OracleDictionary;
                });
    }

    protected SearchSupport.SearchView defaultSV(final SearchSupport svs) {
        return svs.field();
    }

    protected String anyId(final SearchSupport svs) {
        return defaultSV(svs).alias() + ".any_id";
    }

    protected Optional<AnySearchNode> getQueryForCustomConds(
            final SearchCond cond,
            final List<Object> parameters,
            final SearchSupport svs,
            final boolean not) {

        // do nothing by default, leave it open for subclasses
        return Optional.empty();
    }

    protected Optional<Pair<AnySearchNode, Set<String>>> getQuery(
            final SearchCond cond, final List<Object> parameters, final SearchSupport svs) {

        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        Optional<AnySearchNode> node = Optional.empty();
        Set<String> plainSchemas = new HashSet<>();

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                if (node.isEmpty()) {
                    node = cond.asLeaf(AnyTypeCond.class).
                            filter(leaf -> AnyTypeKind.ANY_OBJECT == svs.anyTypeKind).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(AuxClassCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(RelationshipTypeCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(RelationshipCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(MembershipCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(MemberCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(RoleCond.class).
                            filter(leaf -> AnyTypeKind.USER == svs.anyTypeKind).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(DynRealmCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(ResourceCond.class).
                            map(leaf -> getQuery(leaf, not, parameters, svs));
                }

                if (node.isEmpty()) {
                    node = cond.asLeaf(AnyCond.class).
                            map(anyCond -> getQuery(anyCond, not, parameters, svs)).
                            or(() -> cond.asLeaf(AttrCond.class).
                            map(attrCond -> {
                                Pair<PlainSchema, PlainAttrValue> checked = check(attrCond);
                                Pair<Boolean, AnySearchNode> query = getQuery(attrCond, not, checked, parameters, svs);
                                if (query.getLeft()) {
                                    plainSchemas.add(checked.getLeft().getKey());
                                }
                                return query.getRight();
                            }));
                }

                // allow for additional search conditions
                if (node.isEmpty()) {
                    node = getQueryForCustomConds(cond, parameters, svs, not);
                }
                break;

            case AND:
                AnySearchNode andNode = new AnySearchNode(AnySearchNode.Type.AND);

                getQuery(cond.getLeft(), parameters, svs).ifPresent(left -> {
                    andNode.add(left.getLeft());
                    plainSchemas.addAll(left.getRight());
                });

                getQuery(cond.getRight(), parameters, svs).ifPresent(right -> {
                    andNode.add(right.getLeft());
                    plainSchemas.addAll(right.getRight());
                });

                if (!andNode.getChildren().isEmpty()) {
                    node = Optional.of(andNode);
                }
                break;

            case OR:
                AnySearchNode orNode = new AnySearchNode(AnySearchNode.Type.OR);

                getQuery(cond.getLeft(), parameters, svs).ifPresent(left -> {
                    orNode.add(left.getLeft());
                    plainSchemas.addAll(left.getRight());
                });

                getQuery(cond.getRight(), parameters, svs).ifPresent(right -> {
                    orNode.add(right.getLeft());
                    plainSchemas.addAll(right.getRight());
                });

                if (!orNode.getChildren().isEmpty()) {
                    node = Optional.of(orNode);
                }
                break;

            default:
        }

        return node.map(n -> Pair.of(n, plainSchemas));
    }

    protected AnySearchNode getQuery(
            final AnyTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder("type_id");
        if (not) {
            clause.append("<>");
        } else {
            clause.append('=');
        }
        clause.append('?').append(setParameter(parameters, cond.getAnyTypeKey()));

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final AuxClassCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder();
        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }
        clause.append("SELECT any_id FROM ").
                append(svs.auxClass().name()).
                append(" WHERE anyTypeClass_id=?").
                append(setParameter(parameters, cond.getAuxClass())).
                append(')');

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final RelationshipTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder();
        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }
        clause.append("SELECT any_id ").append("FROM ").
                append(svs.relationship().name()).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(" UNION SELECT right_any_id AS any_id FROM ").
                append(svs.relationship().name()).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(')');

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final RelationshipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Set<String> rightAnyObjects = check(cond);

        StringBuilder clause = new StringBuilder();
        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }
        clause.append("SELECT DISTINCT any_id FROM ").
                append(svs.relationship().name()).append(" WHERE ").
                append(rightAnyObjects.stream().
                        map(key -> "right_any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(')');

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final MembershipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        List<String> groupKeys = check(cond);

        String subwhere = groupKeys.stream().
                map(key -> "group_id=?" + setParameter(parameters, key)).
                collect(Collectors.joining(" OR "));

        StringBuilder clause = new StringBuilder("(");

        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }
        clause.append("SELECT DISTINCT any_id FROM ").
                append(svs.membership().name()).append(" WHERE ").
                append(subwhere).
                append(") ");

        if (not) {
            clause.append("AND ").append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append("OR ").append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT any_id FROM ").
                append(svs.dyngroupmembership().name()).append(" WHERE ").
                append(subwhere).
                append("))");

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final RoleCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder("(");

        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT any_id FROM ").
                append(svs.role().name()).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(") ");

        if (not) {
            clause.append("AND ").append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append("OR ").append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT any_id FROM ").
                append(SearchSupport.dynrolemembership().name()).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append("))");

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final DynRealmCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder("(");

        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT any_id FROM ").
                append(SearchSupport.dynrealmmembership().name()).append(" WHERE ").
                append("dynRealm_id=?").append(setParameter(parameters, cond.getDynRealm())).
                append("))");

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final ResourceCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder clause = new StringBuilder();

        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT any_id FROM ").
                append(svs.resource().name()).
                append(" WHERE resource_id=?").
                append(setParameter(parameters, cond.getResource()));

        if (svs.anyTypeKind == AnyTypeKind.USER || svs.anyTypeKind == AnyTypeKind.ANY_OBJECT) {
            clause.append(" UNION SELECT DISTINCT any_id FROM ").
                    append(svs.groupResource().name()).
                    append(" WHERE resource_id=?").
                    append(setParameter(parameters, cond.getResource()));
        }

        clause.append(')');

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode getQuery(
            final MemberCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Set<String> members = check(cond);

        StringBuilder clause = new StringBuilder();

        if (not) {
            clause.append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.USER).membership().name()).append(" WHERE ").
                append(members.stream().
                        map(key -> "any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(") ");

        if (not) {
            clause.append("AND ").append(anyId(svs)).append(" NOT IN (");
        } else {
            clause.append("OR ").append(anyId(svs)).append(" IN (");
        }

        clause.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.ANY_OBJECT).membership().name()).append(" WHERE ").
                append(members.stream().
                        map(key -> "any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(')');

        return new AnySearchNode.Leaf(defaultSV(svs), clause.toString());
    }

    protected AnySearchNode.Leaf fillAttrQuery(
            final String column,
            final SearchSupport.SearchView from,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters) {

        // activate ignoreCase only for EQ and LIKE operators
        boolean ignoreCase = AttrCond.Type.ILIKE == cond.getType() || AttrCond.Type.IEQ == cond.getType();

        String left = column;
        if (ignoreCase && (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)) {
            left = "LOWER(" + left + ')';
        }

        StringBuilder clause = new StringBuilder(left);
        switch (cond.getType()) {

            case ILIKE:
            case LIKE:
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                    if (not) {
                        clause.append(" NOT ");
                    }
                    clause.append(" LIKE ");
                    if (ignoreCase) {
                        clause.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                    } else {
                        clause.append('?').append(setParameter(parameters, cond.getExpression()));
                    }
                    if (isOracle()) {
                        clause.append(" ESCAPE '\\'");
                    }
                } else {
                    LOG.error("LIKE is only compatible with string or enum schemas");
                    return new AnySearchNode.Leaf(from, ALWAYS_FALSE_CLAUSE);
                }
                break;

            case IEQ:
            case EQ:
            default:
                if (not) {
                    clause.append("<>");
                } else {
                    clause.append('=');
                }
                if (ignoreCase
                        && (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)) {

                    clause.append("LOWER(?").append(setParameter(parameters, attrValue.getValue())).append(')');
                } else {
                    clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                }
                break;

            case GE:
                if (not) {
                    clause.append('<');
                } else {
                    clause.append(">=");
                }
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case GT:
                if (not) {
                    clause.append("<=");
                } else {
                    clause.append('>');
                }
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LE:
                if (not) {
                    clause.append('>');
                } else {
                    clause.append("<=");
                }
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LT:
                if (not) {
                    clause.append(">=");
                } else {
                    clause.append('<');
                }
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;
        }

        return new AnySearchNode.Leaf(
                from,
                cond instanceof AnyCond
                        ? clause.toString()
                        : from.alias() + ".schema_id='" + schema.getKey() + "' AND " + clause);
    }

    protected Pair<Boolean, AnySearchNode> getQuery(
            final AttrCond cond,
            final boolean not,
            final Pair<PlainSchema, PlainAttrValue> checked,
            final List<Object> parameters,
            final SearchSupport svs) {

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        SearchSupport.SearchView sv = checked.getLeft().isUniqueConstraint()
                ? svs.asSearchViewSupport().uniqueAttr()
                : svs.asSearchViewSupport().attr();

        switch (cond.getType()) {
            case ISNOTNULL -> {
                return Pair.of(true, new AnySearchNode.Leaf(
                        sv,
                        sv.alias() + ".schema_id='" + checked.getLeft().getKey() + "'"));
            }

            case ISNULL -> {
                String clause = new StringBuilder(anyId(svs)).append(" NOT IN ").
                        append('(').
                        append("SELECT DISTINCT any_id FROM ").
                        append(sv.name()).
                        append(" WHERE schema_id=").append("'").append(checked.getLeft().getKey()).append("'").
                        append(')').toString();
                return Pair.of(true, new AnySearchNode.Leaf(defaultSV(svs), clause));
            }

            default -> {
                AnySearchNode.Leaf node;
                if (not && checked.getLeft().isMultivalue()) {
                    AnySearchNode.Leaf notNode = fillAttrQuery(
                            sv.alias() + "." + key(checked.getLeft().getType()),
                            sv,
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            false,
                            parameters);
                    node = new AnySearchNode.Leaf(
                            sv,
                            anyId(svs) + " NOT IN ("
                            + "SELECT any_id FROM " + sv.name()
                            + " WHERE " + notNode.getClause().replace(sv.alias() + ".", "")
                            + ")");
                } else {
                    node = fillAttrQuery(
                            sv.alias() + "." + key(checked.getLeft().getType()),
                            sv,
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            not,
                            parameters);
                }
                return Pair.of(true, node);
            }
        }
    }

    protected AnySearchNode getQuery(
            final AnyCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())
                && !SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {

            Realm realm = realmSearchDAO.findByFullPath(cond.getExpression()).
                    orElseThrow(() -> new IllegalArgumentException("Invalid Realm full path: " + cond.getExpression()));
            cond.setExpression(realm.getKey());
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, svs.anyTypeKind);

        return switch (checked.getRight().getType()) {
            case ISNULL ->
                new AnySearchNode.Leaf(
                defaultSV(svs),
                checked.getRight().getSchema() + (not ? " IS NOT NULL" : " IS NULL"));

            case ISNOTNULL ->
                new AnySearchNode.Leaf(
                defaultSV(svs),
                checked.getRight().getSchema() + (not ? " IS NULL" : " IS NOT NULL"));

            default ->
                fillAttrQuery(
                checked.getRight().getSchema(),
                defaultSV(svs),
                checked.getMiddle(),
                checked.getLeft(),
                checked.getRight(),
                not,
                parameters);
        };
    }

    protected AnySearchNode.Leaf buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final SearchSupport svs,
            final List<Object> parameters) {

        if (realmKeys.isEmpty()) {
            return new AnySearchNode.Leaf(defaultSV(svs), StringUtils.substringAfter(anyId(svs), '.') + " IS NOT NULL");
        }

        String realmKeysArg = realmKeys.stream().
                map(realmKey -> "?" + setParameter(parameters, realmKey)).
                collect(Collectors.joining(","));
        return new AnySearchNode.Leaf(defaultSV(svs), "realm_id IN (" + realmKeysArg + ")");
    }

    protected Triple<AnySearchNode.Leaf, Set<String>, Set<String>> getAdminRealmsFilter(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final List<Object> parameters,
            final SearchSupport svs) {

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

        return Triple.of(buildAdminRealmsFilter(realmKeys, svs, parameters), dynRealmKeys, groupOwners);
    }

    protected void visitNode(
            final AnySearchNode node,
            final Map<SearchSupport.SearchView, Boolean> counters,
            final Set<SearchSupport.SearchView> from,
            final List<String> where,
            final SearchSupport svs) {

        node.asLeaf().ifPresentOrElse(
                leaf -> {
                    from.add(leaf.getFrom());

                    if (counters.computeIfAbsent(leaf.getFrom(), view -> false) && !leaf.getClause().contains(" IN ")) {
                        where.add(anyId(svs) + " IN ("
                                + "SELECT any_id FROM " + leaf.getFrom().name()
                                + " WHERE " + leaf.getClause().replace(leaf.getFrom().alias() + ".", "")
                                + ")");
                    } else {
                        counters.put(leaf.getFrom(), true);
                        where.add(leaf.getClause());
                    }
                },
                () -> {
                    List<String> nodeWhere = new ArrayList<>();
                    node.getChildren().forEach(child -> visitNode(child, counters, from, nodeWhere, svs));
                    where.add(nodeWhere.stream().
                            map(w -> "(" + w + ")").
                            collect(Collectors.joining(" " + node.getType().name() + " ")));
                });
    }

    protected String buildFrom(
            final Set<SearchSupport.SearchView> from,
            final Set<String> plainSchemas,
            final OrderBySupport obs) {

        String fromString;
        if (from.size() == 1) {
            SearchSupport.SearchView sv = from.iterator().next();
            fromString = sv.name() + " " + sv.alias();
        } else {
            List<SearchSupport.SearchView> joins = new ArrayList<>(from);
            StringBuilder join = new StringBuilder(joins.getFirst().name() + " " + joins.getFirst().alias());
            for (int i = 1; i < joins.size(); i++) {
                SearchSupport.SearchView sv = joins.get(i);
                join.append(" LEFT JOIN ").
                        append(sv.name()).append(" ").append(sv.alias()).
                        append(" ON ").
                        append(joins.getFirst().alias()).append(".any_id=").append(sv.alias()).append(".any_id");
            }
            fromString = join.toString();
        }
        return fromString;
    }

    protected String buildWhere(final List<String> where, final AnySearchNode root) {
        return where.stream().
                map(w -> "(" + w + ")").
                collect(Collectors.joining(" " + root.getType().name() + " "));
    }

    protected String buildCountQuery(
            final Pair<AnySearchNode, Set<String>> queryInfo,
            final AnySearchNode.Leaf filterNode,
            final List<Object> parameters,
            final SearchSupport svs) {

        AnySearchNode root;
        if (queryInfo.getLeft().getType() == AnySearchNode.Type.AND) {
            root = queryInfo.getLeft();
        } else {
            root = new AnySearchNode(AnySearchNode.Type.AND);
            root.add(queryInfo.getLeft());
        }
        root.add(filterNode);

        Set<SearchSupport.SearchView> from = new HashSet<>();
        List<String> where = new ArrayList<>();
        Map<SearchSupport.SearchView, Boolean> counters = new HashMap<>();
        visitNode(root, counters, from, where, svs);

        StringBuilder queryString = new StringBuilder("SELECT COUNT(DISTINCT ").append(anyId(svs)).append(") ");

        queryString.append("FROM ").append(buildFrom(from, queryInfo.getRight(), null));

        queryString.append(" WHERE ").append(buildWhere(where, root));

        LOG.debug("Query: {}, parameters: {}", queryString, parameters);

        return queryString.toString();
    }

    @Override
    protected long doCount(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        List<Object> parameters = new ArrayList<>();

        SearchSupport svs = new SearchViewSupport(kind);

        // 1. get admin realms filter
        Triple<AnySearchNode.Leaf, Set<String>, Set<String>> filter =
                getAdminRealmsFilter(base, recursive, adminRealms, parameters, svs);

        // 2. transform search condition
        Optional<Pair<AnySearchNode, Set<String>>> optionalQueryInfo = getQuery(
                buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), parameters, svs);
        if (optionalQueryInfo.isEmpty()) {
            LOG.error("Invalid search condition: {}", cond);
            return 0;
        }
        Pair<AnySearchNode, Set<String>> queryInfo = optionalQueryInfo.get();

        // 3. generate the query string
        String queryString = buildCountQuery(queryInfo, filter.getLeft(), parameters, svs);

        // 4. populate the search query with parameter values
        Query countQuery = entityManager.createNativeQuery(queryString);
        fillWithParameters(countQuery, parameters);

        // 5. execute the query and return the result
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final Sort.Order clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        if (schema.isUniqueConstraint()) {
            obs.views.add(svs.asSearchViewSupport().uniqueAttr());

            item.select = new StringBuilder().
                    append(svs.asSearchViewSupport().uniqueAttr().alias()).append('.').
                    append(key(schema.getType())).
                    append(" AS ").append(fieldName).toString();
            item.where = new StringBuilder().
                    append(svs.asSearchViewSupport().uniqueAttr().alias()).
                    append(".schema_id='").append(fieldName).append("'").toString();
            item.orderBy = fieldName + ' ' + clause.getDirection().name();
        } else {
            obs.views.add(svs.asSearchViewSupport().attr());

            item.select = new StringBuilder().
                    append(svs.asSearchViewSupport().attr().alias()).append('.').append(key(schema.getType())).
                    append(" AS ").append(fieldName).toString();
            item.where = new StringBuilder().
                    append(svs.asSearchViewSupport().attr().alias()).
                    append(".schema_id='").append(fieldName).append("'").toString();
            item.orderBy = fieldName + ' ' + clause.getDirection().name();
        }
    }

    protected void parseOrderByForField(
            final SearchSupport svs,
            final OrderBySupport.Item item,
            final String fieldName,
            final Sort.Order clause) {

        item.select = defaultSV(svs).alias() + '.' + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = defaultSV(svs).alias() + '.' + fieldName + ' ' + clause.getDirection().name();
    }

    protected void parseOrderByForCustom(
            final SearchSupport svs,
            final Sort.Order clause,
            final OrderBySupport.Item item,
            final OrderBySupport obs) {

        // do nothing by default, meant for subclasses
    }

    protected OrderBySupport parseOrderBy(
            final SearchSupport svs,
            final List<Sort.Order> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(svs.anyTypeKind);

        OrderBySupport obs = new OrderBySupport();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            OrderBySupport.Item item = new OrderBySupport.Item();

            parseOrderByForCustom(svs, clause, item, obs);

            if (item.isEmpty()) {
                anyUtils.getField(clause.getProperty()).ifPresentOrElse(
                        field -> {
                            // Manage difference among external key attribute and internal JPA @Id
                            String fieldName = "key".equals(clause.getProperty()) ? "id" : clause.getProperty();

                            // Adjust field name to column name
                            if (ArrayUtils.contains(RELATIONSHIP_FIELDS, fieldName)) {
                                fieldName += "_id";
                            }

                            obs.views.add(defaultSV(svs));

                            parseOrderByForField(svs, item, fieldName, clause);
                        },
                        () -> {
                            plainSchemaDAO.findById(clause.getProperty()).ifPresent(schema -> {
                                if (schema.isUniqueConstraint()) {
                                    orderByUniquePlainSchemas.add(schema.getKey());
                                } else {
                                    orderByNonUniquePlainSchemas.add(schema.getKey());
                                }
                                if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                                    throw syncopeClientException("Order by more than one attribute is not allowed; "
                                            + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                            ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas)).get();
                                }
                                parseOrderByForPlainSchema(svs, obs, item, clause, schema, clause.getProperty());
                            });
                        });
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        });

        return obs;
    }

    protected String buildSearchQuery(
            final Pair<AnySearchNode, Set<String>> queryInfo,
            final AnySearchNode.Leaf filterNode,
            final List<Object> parameters,
            final SearchSupport svs,
            final List<Sort.Order> orderBy) {

        AnySearchNode root;
        if (queryInfo.getLeft().getType() == AnySearchNode.Type.AND) {
            root = queryInfo.getLeft();
        } else {
            root = new AnySearchNode(AnySearchNode.Type.AND);
            root.add(queryInfo.getLeft());
        }
        root.add(filterNode);

        Set<SearchSupport.SearchView> from = new HashSet<>();
        List<String> where = new ArrayList<>();
        Map<SearchSupport.SearchView, Boolean> counters = new HashMap<>();
        visitNode(root, counters, from, where, svs);

        // 3. take ordering into account
        OrderBySupport obs = parseOrderBy(svs, orderBy);

        // 4. generate the query string
        StringBuilder queryString = new StringBuilder("SELECT DISTINCT ").append(anyId(svs));
        obs.items.forEach(item -> queryString.append(',').append(item.select));

        from.addAll(obs.views);
        queryString.append(" FROM ").append(buildFrom(from, queryInfo.getRight(), obs));

        queryString.append(" WHERE ").append(buildWhere(where, root));

        if (!obs.items.isEmpty()) {
            queryString.append(" ORDER BY ").
                    append(obs.items.stream().map(item -> item.orderBy).collect(Collectors.joining(",")));
        }

        LOG.debug("Query: {}, parameters: {}", queryString, parameters);

        return queryString.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Any> List<T> doSearch(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        List<Object> parameters = new ArrayList<>();

        SearchSupport svs = new SearchViewSupport(kind);

        // 1. get admin realms filter
        Triple<AnySearchNode.Leaf, Set<String>, Set<String>> filter =
                getAdminRealmsFilter(base, recursive, adminRealms, parameters, svs);

        // 2. transform search condition
        Optional<Pair<AnySearchNode, Set<String>>> optionalQueryInfo = getQuery(
                buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), parameters, svs);
        if (optionalQueryInfo.isEmpty()) {
            LOG.error("Invalid search condition: {}", cond);
            return List.of();
        }
        Pair<AnySearchNode, Set<String>> queryInfo = optionalQueryInfo.get();

        // 3. generate the query string
        String queryString = buildSearchQuery(
                queryInfo, filter.getLeft(), parameters, svs, pageable.getSort().toList());

        // 4. prepare the search query
        Query query = entityManager.createNativeQuery(queryString);

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        // 5. populate the search query with parameter values
        fillWithParameters(query, parameters);

        // 6. prepare the result (avoiding duplicates)
        return buildResult(query.getResultList(), kind);
    }
}
