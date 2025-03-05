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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.lib.search.SyncopeFiqlSearchCondition;
import org.apache.syncope.common.lib.search.SyncopeProperty;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchUtils implements Serializable {

    private static final long serialVersionUID = 398381905376547084L;

    public static final Function<SearchClause, CompleteCondition> NO_CUSTOM_CONDITION = clause -> null;

    private static final Logger LOG = LoggerFactory.getLogger(SearchUtils.class);

    private static final BidiMap<String, String> ENCODINGS = new DualHashBidiMap<>() {

        private static final long serialVersionUID = 5636572627689425575L;

        {
            put(",", "%252C");
            put(";", "%253B");
            put("+", "%252B");
        }
    };

    public static Pattern getTypeConditionPattern(final String type) {
        return Pattern.compile(String.format(";\\$type==%s|\\$type==%s;", type, type));
    }

    public static Map<String, List<SearchClause>> getSearchClauses(final Map<String, String> fiql) {
        return fiql.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> getSearchClauses(e.getValue().replaceAll(getTypeConditionPattern(e.getKey()).pattern(), ""))));
    }

    public static List<SearchClause> getSearchClauses(final String fiql) {
        List<SearchClause> clauses = new ArrayList<>();
        if (StringUtils.isNotBlank(fiql)) {
            try {
                SyncopeFiqlParser<SearchBean> fiqlParser = new SyncopeFiqlParser<>(
                        SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);
                clauses.addAll(getSearchClauses(fiqlParser.parse(fiql)));
            } catch (Exception e) {
                LOG.error("Unparseable FIQL expression '{}'", fiql, e);
            }
        }
        return clauses;
    }

    private static List<SearchClause> getSearchClauses(final SearchCondition<SearchBean> sc) {
        List<SearchClause> clauses = new ArrayList<>();

        if (sc.getStatement() == null) {
            clauses.addAll(getCompoundSearchClauses(sc));
        } else {
            clauses.add(getPrimitiveSearchClause(sc));
        }

        return clauses;
    }

    private static List<SearchClause> getCompoundSearchClauses(final SearchCondition<SearchBean> sc) {
        List<SearchClause> clauses = new ArrayList<>();

        sc.getSearchConditions().forEach(searchCondition -> {
            if (searchCondition.getStatement() == null) {
                clauses.addAll(getCompoundSearchClauses(searchCondition));
            } else {
                SearchClause clause = getPrimitiveSearchClause(searchCondition);
                if (sc.getConditionType() == ConditionType.AND) {
                    clause.setOperator(SearchClause.Operator.AND);
                }
                if (sc.getConditionType() == ConditionType.OR) {
                    clause.setOperator(SearchClause.Operator.OR);
                }
                clauses.add(clause);
            }
        });

        return clauses;
    }

    private static SearchClause getPrimitiveSearchClause(final SearchCondition<SearchBean> sc) {
        SearchClause clause = new SearchClause();

        String property = sc.getCondition().getKeySet().iterator().next();
        clause.setProperty(property);

        String value = ENCODINGS.values().stream().
                reduce(sc.getCondition().get(property), (s, v) -> s.replace(v, ENCODINGS.getKey(v)));
        clause.setValue(value);

        LOG.debug("Condition: {}", sc.getCondition());

        if (SpecialAttr.ROLES.toString().equals(property)) {
            clause.setType(SearchClause.Type.ROLE_MEMBERSHIP);
            clause.setProperty(value);
        } else if (SpecialAttr.RELATIONSHIPS.toString().equals(property)) {
            clause.setType(SearchClause.Type.RELATIONSHIP);
            clause.setProperty(value);
        } else if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
            clause.setType(SearchClause.Type.RELATIONSHIP);
            clause.setProperty(value);
        } else if (SpecialAttr.GROUPS.toString().equals(property)) {
            clause.setType(SearchClause.Type.GROUP_MEMBERSHIP);
            clause.setProperty(value);
        } else if (SpecialAttr.AUX_CLASSES.toString().equals(property)) {
            clause.setType(SearchClause.Type.AUX_CLASS);
            clause.setProperty(value);
        } else if (SpecialAttr.RESOURCES.toString().equals(property)) {
            clause.setType(SearchClause.Type.RESOURCE);
            clause.setProperty(value);
        } else if (SpecialAttr.MEMBER.toString().equals(property)) {
            clause.setType(SearchClause.Type.GROUP_MEMBER);
            clause.setProperty(value);
        } else if (property.startsWith("$")) {
            clause.setType(SearchClause.Type.CUSTOM);
            clause.setProperty(value);
        } else {
            clause.setType(SearchClause.Type.ATTRIBUTE);
        }

        ConditionType ct = sc.getConditionType();
        if (sc instanceof final SyncopeFiqlSearchCondition<SearchBean> sfsc
                && sc.getConditionType() == ConditionType.CUSTOM) {
            if (SyncopeFiqlParser.IEQ.equals(sfsc.getOperator())) {
                ct = ConditionType.EQUALS;
            } else if (SyncopeFiqlParser.NIEQ.equals(sfsc.getOperator())) {
                ct = ConditionType.NOT_EQUALS;
            }
        }
        switch (ct) {
            case EQUALS:
                if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
                    clause.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.EQUALS : SearchClause.Comparator.IS_NULL);
                } else {
                    clause.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.IS_NULL : SearchClause.Comparator.EQUALS);
                }
                break;

            case NOT_EQUALS:
                if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
                    clause.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.NOT_EQUALS : SearchClause.Comparator.IS_NOT_NULL);
                } else {
                    clause.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.IS_NOT_NULL : SearchClause.Comparator.NOT_EQUALS);
                }
                break;

            case GREATER_OR_EQUALS:
                clause.setComparator(SearchClause.Comparator.GREATER_OR_EQUALS);
                break;

            case GREATER_THAN:
                clause.setComparator(SearchClause.Comparator.GREATER_THAN);
                break;

            case LESS_OR_EQUALS:
                clause.setComparator(SearchClause.Comparator.LESS_OR_EQUALS);
                break;

            case LESS_THAN:
                clause.setComparator(SearchClause.Comparator.LESS_THAN);
                break;

            default:
                break;
        }

        return clause;
    }

    public static String buildFIQL(
            final List<SearchClause> clauses,
            final AbstractFiqlSearchConditionBuilder<?, ?, ?> builder) {

        return buildFIQL(clauses, builder, Map.of(), NO_CUSTOM_CONDITION);
    }

    public static String buildFIQL(
            final List<SearchClause> clauses,
            final AbstractFiqlSearchConditionBuilder<?, ?, ?> builder,
            final Map<String, PlainSchemaTO> availableSchemaTypes,
            final Function<SearchClause, CompleteCondition> customCondition) {

        LOG.debug("Generating FIQL from {}", clauses);

        CompleteCondition prevCondition;
        CompleteCondition condition = null;

        boolean notTheFirst = false;

        for (SearchClause clause : clauses) {
            prevCondition = condition;

            if (clause.getType() != null) {
                String value = clause.getValue() == null
                        ? null
                        : ENCODINGS.keySet().stream().
                                reduce(clause.getValue(), (s, k) -> s.replace(k, ENCODINGS.get(k)));

                switch (clause.getType()) {
                    case GROUP_MEMBER:
                        if (builder instanceof GroupFiqlSearchConditionBuilder) {
                            switch (clause.getComparator()) {
                                case EQUALS:
                                    condition = ((GroupFiqlSearchConditionBuilder) builder).withMembers(value);
                                    break;

                                case NOT_EQUALS:
                                    condition = ((GroupFiqlSearchConditionBuilder) builder).withoutMembers(value);
                                    break;

                                default:
                            }
                        }
                        break;

                    case GROUP_MEMBERSHIP:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            String groupKey = clause.getProperty();

                            if (builder instanceof final UserFiqlSearchConditionBuilder conditionBuilder) {
                                condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                        ? conditionBuilder.inGroups(groupKey)
                                        : conditionBuilder.notInGroups(groupKey);
                            } else {
                                condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                        ? ((AnyObjectFiqlSearchConditionBuilder) builder).inGroups(groupKey)
                                        : ((AnyObjectFiqlSearchConditionBuilder) builder).notInGroups(groupKey);
                            }
                        }
                        break;

                    case AUX_CLASS:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                    ? builder.hasAuxClasses(clause.getProperty())
                                    : builder.hasNotAuxClasses(clause.getProperty());
                        }
                        break;

                    case RESOURCE:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                    ? builder.hasResources(clause.getProperty())
                                    : builder.hasNotResources(clause.getProperty());
                        }
                        break;

                    case ATTRIBUTE:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            boolean isLong = false;
                            boolean isDouble = false;
                            boolean isBoolean = false;
                            if (availableSchemaTypes.get(clause.getProperty()) != null) {
                                switch (availableSchemaTypes.get(clause.getProperty()).getType()) {
                                    case Long:
                                        isLong = true;
                                        break;

                                    case Double:
                                        isDouble = true;
                                        break;

                                    case Boolean:
                                        isBoolean = true;
                                        break;

                                    default:
                                }
                            }

                            SyncopeProperty<?> property = builder.is(clause.getProperty());
                            switch (clause.getComparator()) {
                                case IS_NULL:
                                    condition = builder.isNull(clause.getProperty());
                                    break;

                                case IS_NOT_NULL:
                                    condition = builder.isNotNull(clause.getProperty());
                                    break;

                                case LESS_THAN:
                                    condition = isLong
                                            ? property.lessThan(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.lessThan(NumberUtils.toDouble(value))
                                                    : property.lexicalBefore(value);
                                    break;

                                case LESS_OR_EQUALS:
                                    condition = isLong
                                            ? property.lessOrEqualTo(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.lessOrEqualTo(NumberUtils.toDouble(value))
                                                    : property.lexicalNotAfter(value);
                                    break;

                                case GREATER_THAN:
                                    condition = isLong
                                            ? property.greaterThan(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.greaterThan(NumberUtils.toDouble(value))
                                                    : property.lexicalAfter(value);
                                    break;

                                case GREATER_OR_EQUALS:
                                    condition = isLong
                                            ? property.greaterOrEqualTo(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.greaterOrEqualTo(NumberUtils.toDouble(value))
                                                    : property.lexicalNotBefore(value);
                                    break;

                                case NOT_EQUALS:
                                    condition = isLong
                                            ? property.notEqualTo(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.notEqualTo(NumberUtils.toDouble(value))
                                                    : isBoolean
                                                            ? property.notEqualTo(BooleanUtils.toStringTrueFalse(
                                                                    BooleanUtils.toBoolean(value)))
                                                            : property.notEqualTolIgnoreCase(value);
                                    break;

                                case EQUALS:
                                    condition = isLong
                                            ? property.equalTo(NumberUtils.toLong(value))
                                            : isDouble
                                                    ? property.equalTo(NumberUtils.toDouble(value))
                                                    : isBoolean
                                                            ? property.equalTo(BooleanUtils.toStringTrueFalse(
                                                                    BooleanUtils.toBoolean(value)))
                                                            : property.equalToIgnoreCase(value);
                                    break;

                                default:
                                    condition = property.equalToIgnoreCase(value);
                                    break;
                            }
                        }
                        break;

                    case ROLE_MEMBERSHIP:
                        if (StringUtils.isNotBlank(clause.getProperty())
                                && builder instanceof UserFiqlSearchConditionBuilder) {

                            switch (clause.getComparator()) {
                                case EQUALS:
                                    condition = ((UserFiqlSearchConditionBuilder) builder).
                                            inRoles(clause.getProperty());
                                    break;
                                case NOT_EQUALS:
                                    condition = ((UserFiqlSearchConditionBuilder) builder).
                                            notInRoles(clause.getProperty());
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;

                    case RELATIONSHIP:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            if (builder instanceof UserFiqlSearchConditionBuilder ubuilder) {
                                switch (clause.getComparator()) {
                                    case IS_NOT_NULL:
                                        condition = ubuilder.inRelationshipTypes(clause.getProperty());
                                        break;
                                    case IS_NULL:
                                        condition = ubuilder.notInRelationshipTypes(clause.getProperty());
                                        break;
                                    case EQUALS:
                                        condition = ubuilder.inRelationships(value);
                                        break;
                                    case NOT_EQUALS:
                                        condition = ubuilder.notInRelationships(value);
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                switch (clause.getComparator()) {
                                    case IS_NOT_NULL:
                                        condition = ((AnyObjectFiqlSearchConditionBuilder) builder).
                                                inRelationshipTypes(clause.getProperty());
                                        break;
                                    case IS_NULL:
                                        condition = ((AnyObjectFiqlSearchConditionBuilder) builder).
                                                notInRelationshipTypes(clause.getProperty());
                                        break;
                                    case EQUALS:
                                        condition = ((AnyObjectFiqlSearchConditionBuilder) builder).
                                                inRelationships(value);
                                        break;
                                    case NOT_EQUALS:
                                        condition = ((AnyObjectFiqlSearchConditionBuilder) builder).
                                                notInRelationships(value);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        break;

                    case CUSTOM:
                        condition = customCondition.apply(clause);
                        break;

                    default:
                        break;
                }
            }

            if (notTheFirst) {
                if (clause.getOperator() == SearchClause.Operator.AND) {
                    condition = builder.and(condition, prevCondition);
                }
                if (clause.getOperator() == SearchClause.Operator.OR) {
                    condition = builder.or(condition, prevCondition);
                }
            }

            notTheFirst = true;
        }

        String fiql = condition == null ? null : condition.query();
        LOG.debug("Generated FIQL: {}", fiql);

        return fiql;
    }

    private SearchUtils() {
        // private constructor for static utility class
    }
}
