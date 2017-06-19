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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchUtils implements Serializable {

    private static final long serialVersionUID = 398381905376547084L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchUtils.class);

    private static Pattern getTypeConditionPattern(final String type) {
        return Pattern.compile(String.format(";\\$type==%s|\\$type==%s;", type, type));
    }

    public static Map<String, List<SearchClause>> getSearchClauses(final Map<String, String> fiql) {
        final Map<String, List<SearchClause>> res = new HashMap<>();
        if (fiql != null && !fiql.isEmpty()) {
            for (Map.Entry<String, String> entry : fiql.entrySet()) {
                res.put(entry.getKey(), getSearchClauses(
                        entry.getValue().replaceAll(getTypeConditionPattern(entry.getKey()).pattern(), "")));
            }
        }
        return res;
    }

    public static List<SearchClause> getSearchClauses(final String fiql) {
        final List<SearchClause> res = new ArrayList<>();
        if (StringUtils.isNotBlank(fiql)) {
            try {
                SyncopeFiqlParser<SearchBean> fiqlParser = new SyncopeFiqlParser<>(
                        SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);
                res.addAll(getSearchClauses(fiqlParser.parse(fiql)));
            } catch (Exception e) {
                LOG.error("Unparseable FIQL expression '{}'", fiql, e);
            }
        }
        return res;
    }

    private static List<SearchClause> getSearchClauses(final SearchCondition<SearchBean> sc) {
        List<SearchClause> res = new ArrayList<>();

        if (sc.getStatement() == null) {
            res.addAll(getCompoundSearchClause(sc));
        } else {
            res.add(getPrimitiveSearchClause(sc));
        }

        return res;
    }

    private static List<SearchClause> getCompoundSearchClause(final SearchCondition<SearchBean> sc) {
        List<SearchClause> res = new ArrayList<>();

        for (SearchCondition<SearchBean> searchCondition : sc.getSearchConditions()) {
            if (searchCondition.getStatement() == null) {
                res.addAll(getCompoundSearchClause(searchCondition));
            } else {
                SearchClause clause = getPrimitiveSearchClause(searchCondition);
                if (sc.getConditionType() == ConditionType.AND) {
                    clause.setOperator(SearchClause.Operator.AND);
                }
                if (sc.getConditionType() == ConditionType.OR) {
                    clause.setOperator(SearchClause.Operator.OR);
                }
                res.add(clause);
            }
        }

        return res;
    }

    private static SearchClause getPrimitiveSearchClause(final SearchCondition<SearchBean> sc) {
        SearchClause res = new SearchClause();

        String property = sc.getCondition().getKeySet().iterator().next();
        res.setProperty(property);
        String value = sc.getCondition().get(property);
        res.setValue(value);

        LOG.debug("Condition: " + sc.getCondition());

        if (SpecialAttr.ROLES.toString().equals(property)) {
            res.setType(SearchClause.Type.ROLE_MEMBERSHIP);
            res.setProperty(value);
        } else if (SpecialAttr.RELATIONSHIPS.toString().equals(property)) {
            res.setType(SearchClause.Type.RELATIONSHIP);
            res.setProperty(value);
        } else if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
            res.setType(SearchClause.Type.RELATIONSHIP);
            res.setProperty(value);
        } else if (SpecialAttr.GROUPS.toString().equals(property)) {
            res.setType(SearchClause.Type.GROUP_MEMBERSHIP);
            res.setProperty(value);
        } else if (SpecialAttr.RESOURCES.toString().equals(property)) {
            res.setType(SearchClause.Type.RESOURCE);
            res.setProperty(value);
        } else if (SpecialAttr.MEMBER.toString().equals(property)) {
            res.setType(SearchClause.Type.GROUP_MEMBER);
            res.setProperty(value);
        } else {
            res.setType(SearchClause.Type.ATTRIBUTE);
        }

        ConditionType ct = sc.getConditionType();
        if (sc instanceof SyncopeFiqlSearchCondition && sc.getConditionType() == ConditionType.CUSTOM) {
            SyncopeFiqlSearchCondition<SearchBean> sfsc = (SyncopeFiqlSearchCondition<SearchBean>) sc;
            if (SyncopeFiqlParser.IEQ.equals(sfsc.getOperator())) {
                ct = ConditionType.EQUALS;
            } else if (SyncopeFiqlParser.NIEQ.equals(sfsc.getOperator())) {
                ct = ConditionType.NOT_EQUALS;
            }
        }
        switch (ct) {
            case EQUALS:
                if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
                    res.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.EQUALS : SearchClause.Comparator.IS_NULL);
                } else {
                    res.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.IS_NULL : SearchClause.Comparator.EQUALS);
                }
                break;

            case NOT_EQUALS:
                if (SpecialAttr.RELATIONSHIP_TYPES.toString().equals(property)) {
                    res.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.NOT_EQUALS : SearchClause.Comparator.IS_NOT_NULL);
                } else {
                    res.setComparator(SpecialAttr.NULL.toString().equals(value)
                            ? SearchClause.Comparator.IS_NOT_NULL : SearchClause.Comparator.NOT_EQUALS);
                }
                break;

            case GREATER_OR_EQUALS:
                res.setComparator(SearchClause.Comparator.GREATER_OR_EQUALS);
                break;

            case GREATER_THAN:
                res.setComparator(SearchClause.Comparator.GREATER_THAN);
                break;

            case LESS_OR_EQUALS:
                res.setComparator(SearchClause.Comparator.LESS_OR_EQUALS);
                break;

            case LESS_THAN:
                res.setComparator(SearchClause.Comparator.LESS_THAN);
                break;

            default:
                break;
        }

        return res;
    }

    public static String buildFIQL(final List<SearchClause> clauses, final AbstractFiqlSearchConditionBuilder builder) {
        return buildFIQL(clauses, builder, Collections.<String, PlainSchemaTO>emptyMap());
    }

    public static String buildFIQL(
            final List<SearchClause> clauses,
            final AbstractFiqlSearchConditionBuilder builder,
            final Map<String, PlainSchemaTO> availableSchemaTypes) {
        LOG.debug("Generating FIQL from List<SearchClause>: {}", clauses);

        CompleteCondition prevCondition;
        CompleteCondition condition = null;

        boolean notTheFirst = false;

        for (SearchClause clause : clauses) {
            prevCondition = condition;

            if (clause.getType() != null) {
                switch (clause.getType()) {
                    case GROUP_MEMBER:
                        switch (clause.getComparator()) {
                            case EQUALS:
                                condition = ((GroupFiqlSearchConditionBuilder) builder).
                                        withMembers(clause.getValue());
                                break;

                            case NOT_EQUALS:
                                condition = ((GroupFiqlSearchConditionBuilder) builder).
                                        withoutMembers(clause.getValue());
                                break;

                            default:
                        }
                        break;

                    case GROUP_MEMBERSHIP:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
                            String groupKey = clause.getProperty().split(" ")[0];

                            if (builder instanceof UserFiqlSearchConditionBuilder) {
                                condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                        ? ((UserFiqlSearchConditionBuilder) builder).inGroups(groupKey)
                                        : ((UserFiqlSearchConditionBuilder) builder).notInGroups(groupKey);
                            } else {
                                condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                        ? ((AnyObjectFiqlSearchConditionBuilder) builder).inGroups(groupKey)
                                        : ((AnyObjectFiqlSearchConditionBuilder) builder).notInGroups(groupKey);
                            }
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
                            boolean isLong = availableSchemaTypes.get(clause.getProperty()) != null
                                    && availableSchemaTypes.get(clause.getProperty()).getType()
                                    == AttrSchemaType.Long;

                            SyncopeProperty property = builder.is(clause.getProperty());
                            switch (clause.getComparator()) {
                                case IS_NULL:
                                    condition = builder.isNull(clause.getProperty());
                                    break;

                                case IS_NOT_NULL:
                                    condition = builder.isNotNull(clause.getProperty());
                                    break;

                                case LESS_THAN:
                                    condition = isLong
                                            ? property.lessThan(NumberUtils.toLong(clause.getValue()))
                                            : property.lexicalBefore(clause.getValue());
                                    break;

                                case LESS_OR_EQUALS:
                                    condition = isLong
                                            ? property.lessOrEqualTo(NumberUtils.toLong(clause.getValue()))
                                            : property.lexicalNotAfter(clause.getValue());
                                    break;

                                case GREATER_THAN:
                                    condition = isLong
                                            ? property.greaterThan(NumberUtils.toLong(clause.getValue()))
                                            : property.lexicalAfter(clause.getValue());
                                    break;

                                case GREATER_OR_EQUALS:
                                    condition = isLong
                                            ? property.greaterOrEqualTo(NumberUtils.toLong(clause.getValue()))
                                            : property.lexicalNotBefore(clause.getValue());
                                    break;

                                case NOT_EQUALS:
                                    condition = property.notEqualTolIgnoreCase(clause.getValue());
                                    break;

                                case EQUALS:
                                default:
                                    condition = property.equalToIgnoreCase(clause.getValue());
                                    break;
                            }
                        }
                        break;

                    case ROLE_MEMBERSHIP:
                        if (StringUtils.isNotBlank(clause.getProperty())) {
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
                            if (builder instanceof UserFiqlSearchConditionBuilder) {
                                switch (clause.getComparator()) {
                                    case IS_NOT_NULL:
                                        condition = ((UserFiqlSearchConditionBuilder) builder).
                                                inRelationshipTypes(clause.getProperty());
                                        break;
                                    case IS_NULL:
                                        condition = ((UserFiqlSearchConditionBuilder) builder).
                                                notInRelationshipTypes(clause.getProperty());
                                        break;
                                    case EQUALS:
                                        condition = ((UserFiqlSearchConditionBuilder) builder).
                                                inRelationships(clause.getValue());
                                        break;
                                    case NOT_EQUALS:
                                        condition = ((UserFiqlSearchConditionBuilder) builder).
                                                notInRelationships(clause.getValue());
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
                                                inRelationships(clause.getValue());
                                        break;
                                    case NOT_EQUALS:
                                        condition = ((AnyObjectFiqlSearchConditionBuilder) builder).
                                                notInRelationships(clause.getValue());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        break;

                    default:
                        break;
                }
            }

            if (notTheFirst) {
                if (clause.getOperator() == SearchClause.Operator.AND) {
                    condition = builder.and(prevCondition, condition);
                }
                if (clause.getOperator() == SearchClause.Operator.OR) {
                    condition = builder.or(prevCondition, condition);
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
