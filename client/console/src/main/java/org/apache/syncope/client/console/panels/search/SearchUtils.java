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
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.SyncopeProperty;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
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
                FiqlParser<SearchBean> fiqlParser = new FiqlParser<>(
                        SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);
                res.addAll(getSearchClauses(fiqlParser.parse(fiql)));
            } catch (Exception e) {
                LOG.error("Unparseable FIQL expression '{}'", fiql, e);
            }
        }
        return res;
    }

    public static List<SearchClause> getSearchClauses(final SearchCondition<SearchBean> sc) {
        List<SearchClause> res = new ArrayList<>();

        if (sc.getStatement() == null) {
            res.addAll(getCompoundSearchClause(sc));
        } else {
            res.add(getPrimitiveSearchClause(sc));
        }

        return res;
    }

    public static List<SearchClause> getCompoundSearchClause(final SearchCondition<SearchBean> sc) {
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

    public static SearchClause getPrimitiveSearchClause(final SearchCondition<SearchBean> sc) {
        SearchClause res = new SearchClause();

        String property = sc.getCondition().getKeySet().iterator().next();
        res.setProperty(property);
        String value = sc.getCondition().get(property);
        res.setValue(value);

        LOG.info("Condition: " + sc.getCondition());

        if (SpecialAttr.GROUPS.toString().equals(property)) {
            res.setType(SearchClause.Type.MEMBERSHIP);
            res.setProperty(value);
        } else if (SpecialAttr.RESOURCES.toString().equals(property)) {
            res.setType(SearchClause.Type.RESOURCE);
            res.setProperty(value);
        } else {
            res.setType(SearchClause.Type.ATTRIBUTE);
        }

        switch (sc.getConditionType()) {
            case EQUALS:
                res.setComparator(SpecialAttr.NULL.toString().equals(value)
                        ? SearchClause.Comparator.IS_NULL : SearchClause.Comparator.EQUALS);
                break;

            case NOT_EQUALS:
                res.setComparator(SpecialAttr.NULL.toString().equals(value)
                        ? SearchClause.Comparator.IS_NOT_NULL : SearchClause.Comparator.NOT_EQUALS);
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
        LOG.debug("Generating FIQL from List<SearchClause>: {}", clauses);

        CompleteCondition prevCondition;
        CompleteCondition condition = null;

        boolean notTheFirst = false;

        for (SearchClause clause : clauses) {
            if (clause.getType() != null && StringUtils.isNotBlank(clause.getProperty())) {
                prevCondition = condition;

                switch (clause.getType()) {
                    case MEMBERSHIP:
                        Long groupId = NumberUtils.toLong(clause.getProperty().split(" ")[0]);

                        if (builder instanceof UserFiqlSearchConditionBuilder) {
                            condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                    ? ((UserFiqlSearchConditionBuilder) builder).inGroups(groupId)
                                    : ((UserFiqlSearchConditionBuilder) builder).notInGroups(groupId);
                        } else {
                            condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                    ? ((AnyObjectFiqlSearchConditionBuilder) builder).inGroups(groupId)
                                    : ((AnyObjectFiqlSearchConditionBuilder) builder).notInGroups(groupId);
                        }
                        break;

                    case RESOURCE:
                        condition = clause.getComparator() == SearchClause.Comparator.EQUALS
                                ? builder.hasResources(clause.getProperty())
                                : builder.hasNotResources(clause.getProperty());
                        break;

                    case ATTRIBUTE:
                        SyncopeProperty property = builder.is(clause.getProperty());
                        switch (clause.getComparator()) {
                            case IS_NULL:
                                condition = builder.isNull(clause.getProperty());
                                break;

                            case IS_NOT_NULL:
                                condition = builder.isNotNull(clause.getProperty());
                                break;

                            case LESS_THAN:
                                condition = StringUtils.isNumeric(clause.getProperty())
                                        ? property.lessThan(NumberUtils.toDouble(clause.getValue()))
                                        : property.lexicalBefore(clause.getValue());
                                break;

                            case LESS_OR_EQUALS:
                                condition = StringUtils.isNumeric(clause.getProperty())
                                        ? property.lessOrEqualTo(NumberUtils.toDouble(clause.getValue()))
                                        : property.lexicalNotAfter(clause.getValue());
                                break;

                            case GREATER_THAN:
                                condition = StringUtils.isNumeric(clause.getProperty())
                                        ? property.greaterThan(NumberUtils.toDouble(clause.getValue()))
                                        : property.lexicalAfter(clause.getValue());
                                break;

                            case GREATER_OR_EQUALS:
                                condition = StringUtils.isNumeric(clause.getProperty())
                                        ? property.greaterOrEqualTo(NumberUtils.toDouble(clause.getValue()))
                                        : property.lexicalNotBefore(clause.getValue());
                                break;

                            case NOT_EQUALS:
                                condition = property.notEqualTo(clause.getValue());
                                break;

                            case EQUALS:
                            default:
                                condition = property.equalTo(clause.getValue());
                                break;
                        }
                        break;
                    default:
                        break;
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
        }

        String fiql = condition == null ? null : condition.query();
        LOG.debug("Generated FIQL: {}", fiql);

        return fiql;
    }

    private SearchUtils() {
        // private constructor for static utility class
    }

}
