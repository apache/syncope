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
package org.apache.syncope.core.rest.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.syncope.common.search.SearchableFields;
import org.apache.syncope.common.search.SpecialAttr;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.dao.search.AttributableCond;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.EntitlementCond;
import org.apache.syncope.core.persistence.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.dao.search.SearchCond;

/**
 * Converts CXF's <tt>SearchCondition</tt> into internal <tt>SearchCond</tt>.
 */
public class SearchCondVisitor extends AbstractSearchConditionVisitor<SearchBean, SearchCond> {

    private static final List<String> ATTRIBUTABLE_FIELDS;

    static {
        ATTRIBUTABLE_FIELDS = new ArrayList<String>();
        ATTRIBUTABLE_FIELDS.addAll(SearchableFields.get(UserTO.class));
        ATTRIBUTABLE_FIELDS.addAll(SearchableFields.get(RoleTO.class));
    }

    private SearchCond searchCond;

    public SearchCondVisitor() {
        super(null);
    }

    public SearchCondVisitor(final Map<String, String> fieldMap) {
        super(fieldMap);
    }

    private AttributeCond createAttributeCond(final String schema) {
        AttributeCond attributeCond = ATTRIBUTABLE_FIELDS.contains(schema)
                ? new AttributableCond()
                : new AttributeCond();
        attributeCond.setSchema(schema);
        return attributeCond;
    }

    private SearchCond visitPrimitive(final SearchCondition<SearchBean> sc) {
        String name = getRealPropertyName(sc.getStatement().getProperty());
        SpecialAttr specialAttrName = SpecialAttr.fromString(name);

        String value = SearchUtils.toSqlWildcardString(sc.getStatement().getValue().toString(), false);
        SpecialAttr specialAttrValue = SpecialAttr.fromString(value);

        AttributeCond attributeCond = createAttributeCond(name);
        attributeCond.setExpression(value);

        SearchCond leaf;
        switch (sc.getConditionType()) {
            case EQUALS:
            case NOT_EQUALS:
                if (specialAttrName == null) {
                    if (specialAttrValue != null && specialAttrValue == SpecialAttr.NULL) {
                        attributeCond.setType(AttributeCond.Type.ISNULL);
                        attributeCond.setExpression(null);
                    } else if (value.indexOf('%') == -1) {
                        attributeCond.setType(AttributeCond.Type.EQ);
                    } else {
                        attributeCond.setType(AttributeCond.Type.LIKE);
                    }

                    leaf = SearchCond.getLeafCond(attributeCond);
                } else {
                    switch (specialAttrName) {
                        case ROLES:
                            MembershipCond membershipCond = new MembershipCond();
                            membershipCond.setRoleId(Long.valueOf(value));
                            leaf = SearchCond.getLeafCond(membershipCond);
                            break;

                        case RESOURCES:
                            ResourceCond resourceCond = new ResourceCond();
                            resourceCond.setResourceName(value);
                            leaf = SearchCond.getLeafCond(resourceCond);
                            break;

                        case ENTITLEMENTS:
                            EntitlementCond entitlementCond = new EntitlementCond();
                            entitlementCond.setExpression(value);
                            leaf = SearchCond.getLeafCond(entitlementCond);
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    String.format("Special attr name %s is not supported", specialAttrName));
                    }
                }
                if (sc.getConditionType() == ConditionType.NOT_EQUALS) {
                    if (leaf.getAttributeCond() != null
                            && leaf.getAttributeCond().getType() == AttributeCond.Type.ISNULL) {

                        leaf.getAttributeCond().setType(AttributeCond.Type.ISNOTNULL);
                    } else if (leaf.getAttributableCond() != null
                            && leaf.getAttributableCond().getType() == AttributableCond.Type.ISNULL) {

                        leaf.getAttributableCond().setType(AttributeCond.Type.ISNOTNULL);
                    } else {
                        leaf = SearchCond.getNotLeafCond(leaf);
                    }
                }
                break;

            case GREATER_OR_EQUALS:
                attributeCond.setType(AttributeCond.Type.GE);
                leaf = SearchCond.getLeafCond(attributeCond);
                break;

            case GREATER_THAN:
                attributeCond.setType(AttributeCond.Type.GT);
                leaf = SearchCond.getLeafCond(attributeCond);
                break;

            case LESS_OR_EQUALS:
                attributeCond.setType(AttributeCond.Type.LE);
                leaf = SearchCond.getLeafCond(attributeCond);
                break;

            case LESS_THAN:
                attributeCond.setType(AttributeCond.Type.LT);
                leaf = SearchCond.getLeafCond(attributeCond);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Condition type %s is not supported", sc.getConditionType().name()));
        }

        return leaf;
    }

    private SearchCond visitCompount(final SearchCondition<SearchBean> sc) {
        List<SearchCond> searchConds = new ArrayList<SearchCond>();
        for (SearchCondition<SearchBean> searchCondition : sc.getSearchConditions()) {
            searchConds.add(searchCondition.getStatement() == null
                    ? visitCompount(searchCondition)
                    : visitPrimitive(searchCondition));
        }

        SearchCond compound;
        switch (sc.getConditionType()) {
            case AND:
                compound = SearchCond.getAndCond(searchConds);
                break;

            case OR:
                compound = SearchCond.getOrCond(searchConds);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Condition type %s is not supported", sc.getConditionType().name()));
        }

        return compound;
    }

    @Override
    public void visit(final SearchCondition<SearchBean> sc) {
        searchCond = sc.getStatement() == null
                ? visitCompount(sc)
                : visitPrimitive(sc);
    }

    @Override
    public SearchCond getQuery() {
        return searchCond;
    }

}
