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
package org.apache.syncope.core.logic.scim;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.type.Resource;

/**
 * Visits SCIM filter expression and produces {@link SearchCond}.
 */
public class SearchCondVisitor extends SCIMFilterBaseVisitor<SearchCond> {

    @Override
    public SearchCond visitScimFilter(final SCIMFilterParser.ScimFilterContext ctx) {
        return visit(ctx.expression(0));
    }

    private AttributeCond createAttributeCond(final String schema) {
        AttributeCond attributeCond;
        if ("userName".equalsIgnoreCase(schema)
                || (Resource.User.schema() + ":userName").equalsIgnoreCase(schema)) {

            attributeCond = new AnyCond();
            attributeCond.setSchema("username");
        } else if ("displayName".equalsIgnoreCase(schema)
                || (Resource.Group.schema() + ":displayName").equalsIgnoreCase(schema)) {

            attributeCond = new AnyCond();
            attributeCond.setSchema("name");
        } else if ("meta.created".equals(schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("creationDate");
        } else if ("meta.lastModified".equals(schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("lastChangeDate");
        } else {
            attributeCond = new AttributeCond();
            attributeCond.setSchema(schema);
        }

        return attributeCond;
    }

    private SearchCond transform(final String operator, final String left, final String right) {
        AttributeCond attributeCond = createAttributeCond(left);
        attributeCond.setExpression(StringUtils.strip(right, "\""));

        switch (operator) {
            case "eq":
            default:
                attributeCond.setType(AttributeCond.Type.IEQ);
                break;

            case "ne":
                attributeCond.setType(AttributeCond.Type.IEQ);
                break;

            case "sw":
                attributeCond.setType(AttributeCond.Type.ILIKE);
                attributeCond.setExpression(attributeCond.getExpression() + "%");
                break;

            case "co":
                attributeCond.setType(AttributeCond.Type.ILIKE);
                attributeCond.setExpression("%" + attributeCond.getExpression() + "%");
                break;

            case "ew":
                attributeCond.setType(AttributeCond.Type.ILIKE);
                attributeCond.setExpression("%" + attributeCond.getExpression());
                break;

            case "gt":
                attributeCond.setType(AttributeCond.Type.GT);
                break;

            case "ge":
                attributeCond.setType(AttributeCond.Type.GE);
                break;

            case "lt":
                attributeCond.setType(AttributeCond.Type.LT);
                break;

            case "le":
                attributeCond.setType(AttributeCond.Type.LE);
                break;

        }

        return "ne".equals(operator)
                ? SearchCond.getNotLeafCond(attributeCond)
                : SearchCond.getLeafCond(attributeCond);
    }

    @Override
    public SearchCond visitEXPR_OPER_EXPR(final SCIMFilterParser.EXPR_OPER_EXPRContext ctx) {
        return transform(ctx.operator().getText(), ctx.expression(0).getText(), ctx.expression(1).getText());
    }

    @Override
    public SearchCond visitATTR_OPER_CRITERIA(final SCIMFilterParser.ATTR_OPER_CRITERIAContext ctx) {
        return transform(ctx.operator().getText(), ctx.ATTRNAME().getText(), ctx.criteria().getText());
    }

    @Override
    public SearchCond visitATTR_OPER_EXPR(final SCIMFilterParser.ATTR_OPER_EXPRContext ctx) {
        return transform(ctx.operator().getText(), ctx.ATTRNAME().getText(), ctx.expression().getText());
    }

    @Override
    public SearchCond visitATTR_PR(final SCIMFilterParser.ATTR_PRContext ctx) {
        AttributeCond cond = createAttributeCond(ctx.ATTRNAME().getText());
        cond.setType(AttributeCond.Type.ISNOTNULL);
        return SearchCond.getLeafCond(cond);
    }

    @Override
    public SearchCond visitLPAREN_EXPR_RPAREN(final SCIMFilterParser.LPAREN_EXPR_RPARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public SearchCond visitNOT_EXPR(final SCIMFilterParser.NOT_EXPRContext ctx) {
        SearchCond cond = visit(ctx.expression());
        if (cond.getAttributeCond() != null) {
            if (cond.getAttributeCond().getType() == AttributeCond.Type.ISNULL) {
                cond.getAttributeCond().setType(AttributeCond.Type.ISNOTNULL);
            } else if (cond.getAttributeCond().getType() == AttributeCond.Type.ISNOTNULL) {
                cond.getAttributeCond().setType(AttributeCond.Type.ISNULL);
            }
        } else if (cond.getAnyCond() != null) {
            if (cond.getAnyCond().getType() == AnyCond.Type.ISNULL) {
                cond.getAnyCond().setType(AnyCond.Type.ISNOTNULL);
            } else if (cond.getAnyCond().getType() == AnyCond.Type.ISNOTNULL) {
                cond.getAnyCond().setType(AnyCond.Type.ISNULL);
            }
        } else {
            cond = SearchCond.getNotLeafCond(cond);
        }

        return cond;
    }

    @Override
    public SearchCond visitEXPR_AND_EXPR(final SCIMFilterParser.EXPR_AND_EXPRContext ctx) {
        return SearchCond.getAndCond(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }

    @Override
    public SearchCond visitEXPR_OR_EXPR(final SCIMFilterParser.EXPR_OR_EXPRContext ctx) {
        return SearchCond.getOrCond(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }

}
