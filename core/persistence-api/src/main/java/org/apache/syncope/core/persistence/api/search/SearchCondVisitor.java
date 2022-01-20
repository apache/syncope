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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.lib.search.SyncopeFiqlSearchCondition;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;

/**
 * Visits CXF's {@link SearchBean} and produces {@link SearchCond}.
 */
public class SearchCondVisitor extends AbstractSearchConditionVisitor<SearchBean, SearchCond> {

    protected static final Pattern TIMEZONE = Pattern.compile(".* [0-9]{4}$");

    protected static final ThreadLocal<String> REALM = new ThreadLocal<>();

    protected static final ThreadLocal<SearchCond> SEARCH_COND = new ThreadLocal<>();

    public SearchCondVisitor() {
        super(null);
    }

    public void setRealm(final String realm) {
        REALM.set(realm);
    }

    protected static AttrCond createAttrCond(final String schema) {
        AttrCond attrCond = SearchableFields.contains(schema)
                ? new AnyCond()
                : new AttrCond();
        attrCond.setSchema(schema);
        return attrCond;
    }

    protected static String getValue(final SearchCondition<SearchBean> sc) {
        String value = SearchUtils.toSqlWildcardString(
                URLDecoder.decode(sc.getStatement().getValue().toString(), StandardCharsets.UTF_8), false).
                replaceAll("\\\\_", "_");

        // see SYNCOPE-1321
        if (TIMEZONE.matcher(value).matches()) {
            char[] valueAsArray = value.toCharArray();
            valueAsArray[valueAsArray.length - 5] = '+';
            value = new String(valueAsArray);
        }

        return value;
    }

    protected static ConditionType getConditionType(final SearchCondition<SearchBean> sc) {
        ConditionType ct = sc.getConditionType();
        if (sc instanceof SyncopeFiqlSearchCondition && sc.getConditionType() == ConditionType.CUSTOM) {
            SyncopeFiqlSearchCondition<SearchBean> sfsc = (SyncopeFiqlSearchCondition<SearchBean>) sc;
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

        return ct;
    }

    @SuppressWarnings("ConvertToStringSwitch")
    protected SearchCond visitPrimitive(final SearchCondition<SearchBean> sc) {
        String name = getRealPropertyName(sc.getStatement().getProperty());
        Optional<SpecialAttr> specialAttrName = SpecialAttr.fromString(name);

        String value = getValue(sc);
        Optional<SpecialAttr> specialAttrValue = SpecialAttr.fromString(value);

        AttrCond attrCond = createAttrCond(name);
        attrCond.setExpression(value);

        ConditionType ct = getConditionType(sc);

        SearchCond leaf;
        switch (ct) {
            case EQUALS:
            case NOT_EQUALS:
                if (specialAttrName.isEmpty()) {
                    if (specialAttrValue.isPresent() && specialAttrValue.get() == SpecialAttr.NULL) {
                        attrCond.setType(AttrCond.Type.ISNULL);
                        attrCond.setExpression(null);
                    } else if (value.indexOf('%') == -1) {
                        attrCond.setType(sc.getConditionType() == ConditionType.CUSTOM
                                ? AttrCond.Type.IEQ
                                : AttrCond.Type.EQ);
                    } else {
                        attrCond.setType(sc.getConditionType() == ConditionType.CUSTOM
                                ? AttrCond.Type.ILIKE
                                : AttrCond.Type.LIKE);
                    }

                    leaf = SearchCond.getLeaf(attrCond);
                } else {
                    switch (specialAttrName.get()) {
                        case TYPE:
                            AnyTypeCond typeCond = new AnyTypeCond();
                            typeCond.setAnyTypeKey(value);
                            leaf = SearchCond.getLeaf(typeCond);
                            break;

                        case RESOURCES:
                            ResourceCond resourceCond = new ResourceCond();
                            resourceCond.setResourceKey(value);
                            leaf = SearchCond.getLeaf(resourceCond);
                            break;

                        case GROUPS:
                            MembershipCond groupCond = new MembershipCond();
                            groupCond.setGroup(value);
                            leaf = SearchCond.getLeaf(groupCond);
                            break;

                        case RELATIONSHIPS:
                            RelationshipCond relationshipCond = new RelationshipCond();
                            relationshipCond.setAnyObject(value);
                            leaf = SearchCond.getLeaf(relationshipCond);
                            break;

                        case RELATIONSHIP_TYPES:
                            RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
                            relationshipTypeCond.setRelationshipTypeKey(value);
                            leaf = SearchCond.getLeaf(relationshipTypeCond);
                            break;

                        case ROLES:
                            RoleCond roleCond = new RoleCond();
                            roleCond.setRole(value);
                            leaf = SearchCond.getLeaf(roleCond);
                            break;

                        case PRIVILEGES:
                            PrivilegeCond privilegeCond = new PrivilegeCond();
                            privilegeCond.setPrivilege(value);
                            leaf = SearchCond.getLeaf(privilegeCond);
                            break;

                        case DYNREALMS:
                            DynRealmCond dynRealmCond = new DynRealmCond();
                            dynRealmCond.setDynRealm(value);
                            leaf = SearchCond.getLeaf(dynRealmCond);
                            break;

                        case ASSIGNABLE:
                            AssignableCond assignableCond = new AssignableCond();
                            assignableCond.setRealmFullPath(REALM.get());
                            leaf = SearchCond.getLeaf(assignableCond);
                            break;

                        case MEMBER:
                            MemberCond memberCond = new MemberCond();
                            memberCond.setMember(value);
                            leaf = SearchCond.getLeaf(memberCond);
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    String.format("Special attr name %s is not supported", specialAttrName));
                    }
                }
                if (ct == ConditionType.NOT_EQUALS) {
                    Optional<AttrCond> notEquals = leaf.getLeaf(AttrCond.class);
                    if (notEquals.isPresent() && notEquals.get().getType() == AttrCond.Type.ISNULL) {
                        notEquals.get().setType(AttrCond.Type.ISNOTNULL);
                    } else {
                        leaf = SearchCond.getNotLeaf(leaf);
                    }
                }
                break;

            case GREATER_OR_EQUALS:
                attrCond.setType(AttrCond.Type.GE);
                leaf = SearchCond.getLeaf(attrCond);
                break;

            case GREATER_THAN:
                attrCond.setType(AttrCond.Type.GT);
                leaf = SearchCond.getLeaf(attrCond);
                break;

            case LESS_OR_EQUALS:
                attrCond.setType(AttrCond.Type.LE);
                leaf = SearchCond.getLeaf(attrCond);
                break;

            case LESS_THAN:
                attrCond.setType(AttrCond.Type.LT);
                leaf = SearchCond.getLeaf(attrCond);
                break;

            default:
                throw new IllegalArgumentException(String.format("Condition type %s is not supported", ct.name()));
        }

        // SYNCOPE-1293: explicitly re-process to allow 'token==$null' or 'token!=$null'
        Optional<AttrCond> reprocess = leaf.getLeaf(AttrCond.class).
                filter(cond -> "token".equals(cond.getSchema())
                && (cond.getType() == AttrCond.Type.ISNULL || cond.getType() == AttrCond.Type.ISNOTNULL)
                && cond.getExpression() == null);
        if (reprocess.isPresent()) {
            AnyCond tokenCond = new AnyCond();
            tokenCond.setSchema(reprocess.get().getSchema());
            tokenCond.setType(reprocess.get().getType());
            tokenCond.setExpression(null);
            leaf = SearchCond.getLeaf(tokenCond);
        }

        return leaf;
    }

    protected SearchCond visitCompound(final SearchCondition<SearchBean> sc) {
        List<SearchCond> searchConds = new ArrayList<>();
        sc.getSearchConditions().forEach(searchCond -> {
            searchConds.add(searchCond.getStatement() == null
                    ? visitCompound(searchCond)
                    : visitPrimitive(searchCond));
        });

        SearchCond compound;
        switch (sc.getConditionType()) {
            case AND:
                compound = SearchCond.getAnd(searchConds);
                break;

            case OR:
                compound = SearchCond.getOr(searchConds);
                break;

            default:
                throw new IllegalArgumentException(
                        String.format("Condition type %s is not supported", sc.getConditionType().name()));
        }

        return compound;
    }

    @Override
    public void visit(final SearchCondition<SearchBean> sc) {
        SEARCH_COND.set(sc.getStatement() == null ? visitCompound(sc) : visitPrimitive(sc));
    }

    @Override
    public SearchCond getQuery() {
        return SEARCH_COND.get();
    }
}
