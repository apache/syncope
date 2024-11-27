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
package org.apache.syncope.core.persistence.api.search;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.lib.search.SyncopeFiqlSearchCondition;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

public class FilterVisitor extends AbstractSearchConditionVisitor<SearchBean, Filter> {

    private Filter filter;

    private final Set<String> attrs = new HashSet<>();

    public FilterVisitor() {
        super(null);
    }

    private Filter visitPrimitive(final SearchCondition<SearchBean> sc) {
        String name = getRealPropertyName(sc.getStatement().getProperty());
        Optional<SpecialAttr> specialAttrName = SpecialAttr.fromString(name);

        String value = SearchUtils.toSqlWildcardString(
                URLDecoder.decode(sc.getStatement().getValue().toString(), StandardCharsets.UTF_8), false).
                replaceAll("\\\\_", "_");
        Optional<SpecialAttr> specialAttrValue = SpecialAttr.fromString(value);

        ConditionType ct = sc.getConditionType();
        if (sc instanceof final SyncopeFiqlSearchCondition<SearchBean> sfsc
            && sc.getConditionType() == ConditionType.CUSTOM) {
            switch (sfsc.getOperator()) {
                case SyncopeFiqlParser.IEQ:
                    ct = ConditionType.EQUALS;
                    break;

                case SyncopeFiqlParser.NIEQ:
                    ct = ConditionType.NOT_EQUALS;
                    break;

                default:
                    throw new IllegalArgumentException(
                            String.format("Condition type %s is not supported", sfsc.getOperator()));
            }
        }

        Attribute attr = AttributeBuilder.build(name, value);
        attrs.add(name);

        Filter leaf;
        switch (ct) {
            case EQUALS:
            case NOT_EQUALS:
                if (specialAttrName.isEmpty()) {
                    if (specialAttrValue.isPresent() && specialAttrValue.get() == SpecialAttr.NULL) {
                        Filter empty = FilterBuilder.startsWith(AttributeBuilder.build(name, StringUtils.EMPTY));
                        if (ct == ConditionType.NOT_EQUALS) {
                            leaf = empty;
                        } else {
                            leaf = FilterBuilder.not(empty);
                            attrs.remove(name);
                        }
                    } else {
                        if (value.indexOf('%') == -1) {
                            leaf = sc.getConditionType() == ConditionType.CUSTOM
                                    ? FilterBuilder.equalsIgnoreCase(attr)
                                    : FilterBuilder.equalTo(attr);
                        } else if (sc.getConditionType() != ConditionType.CUSTOM && value.startsWith("%")) {
                            leaf = FilterBuilder.endsWith(
                                    AttributeBuilder.build(name, value.substring(1)));
                        } else if (sc.getConditionType() != ConditionType.CUSTOM && value.endsWith("%")) {
                            leaf = FilterBuilder.startsWith(
                                    AttributeBuilder.build(name, value.substring(0, value.length() - 1)));
                        } else {
                            throw new IllegalArgumentException(
                                    String.format("Unsupported search value %s", value));
                        }

                        if (ct == ConditionType.NOT_EQUALS) {
                            leaf = FilterBuilder.not(leaf);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Special attr name %s is not supported", specialAttrName));
                }
                break;

            case GREATER_OR_EQUALS:
                leaf = FilterBuilder.greaterThanOrEqualTo(attr);
                break;

            case GREATER_THAN:
                leaf = FilterBuilder.greaterThan(attr);
                break;

            case LESS_OR_EQUALS:
                leaf = FilterBuilder.lessThanOrEqualTo(attr);
                break;

            case LESS_THAN:
                leaf = FilterBuilder.lessThan(attr);
                break;

            default:
                throw new IllegalArgumentException(String.format("Condition type %s is not supported", ct.name()));
        }

        return leaf;
    }

    private Filter visitCompount(final SearchCondition<SearchBean> sc) {
        List<Filter> searchConds = new ArrayList<>();
        sc.getSearchConditions().forEach(searchCond -> {
            searchConds.add(searchCond.getStatement() == null
                    ? visitCompount(searchCond)
                    : visitPrimitive(searchCond));
        });

        Filter compound;
        switch (sc.getConditionType()) {
            case AND:
                compound = FilterBuilder.and(searchConds);
                break;

            case OR:
                compound = FilterBuilder.or(searchConds);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Condition type %s is not supported", sc.getConditionType().name()));
        }

        return compound;
    }

    @Override
    public void visit(final SearchCondition<SearchBean> sc) {
        filter = sc.getStatement() == null
                ? visitCompount(sc)
                : visitPrimitive(sc);
    }

    @Override
    public Filter getQuery() {
        return filter;
    }

    public Set<String> getAttrs() {
        return attrs;
    }
}
