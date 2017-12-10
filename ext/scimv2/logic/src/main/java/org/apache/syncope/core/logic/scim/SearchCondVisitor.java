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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserAddressConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.type.Resource;

/**
 * Visits SCIM filter expression and produces {@link SearchCond}.
 */
public class SearchCondVisitor extends SCIMFilterBaseVisitor<SearchCond> {

    private static final List<String> MULTIVALUE = Arrays.asList(
            "emails", "phoneNumbers", "ims", "photos", "addresses");

    private final Resource resource;

    private final SCIMConf conf;

    public SearchCondVisitor(final Resource resource, final SCIMConf conf) {
        this.resource = resource;
        this.conf = conf;
    }

    @Override
    public SearchCond visitScimFilter(final SCIMFilterParser.ScimFilterContext ctx) {
        return visit(ctx.expression(0));
    }

    private boolean schemaEquals(final Resource resource, final String value, final String schema) {
        return resource == null
                ? value.contains(":")
                ? StringUtils.substringAfterLast(value, ":").equalsIgnoreCase(schema)
                : value.equalsIgnoreCase(schema)
                : value.equalsIgnoreCase(schema) || (resource.schema() + ":" + value).equalsIgnoreCase(schema);
    }

    public AttributeCond createAttributeCond(final String schema) {
        AttributeCond attributeCond = null;

        if (schemaEquals(Resource.User, "userName", schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("username");
        } else if (resource == Resource.Group && schemaEquals(Resource.Group, "displayName", schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("name");
        } else if (schemaEquals(null, "meta.created", schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("creationDate");
        } else if (schemaEquals(null, "meta.lastModified", schema)) {
            attributeCond = new AnyCond();
            attributeCond.setSchema("lastChangeDate");
        }

        if (resource == Resource.User) {
            if (conf.getUserConf() != null) {
                if (conf.getUserConf().getName() != null) {
                    for (Map.Entry<String, String> entry : conf.getUserConf().getName().asMap().entrySet()) {
                        if (schemaEquals(Resource.User, "name." + entry.getKey(), schema)) {
                            attributeCond = new AttributeCond();
                            attributeCond.setSchema(entry.getValue());
                        }
                    }
                }

                for (Map.Entry<String, String> entry : conf.getUserConf().asMap().entrySet()) {
                    if (schemaEquals(Resource.User, entry.getKey(), schema)) {
                        attributeCond = new AttributeCond();
                        attributeCond.setSchema(entry.getValue());
                    }
                }

                for (SCIMUserAddressConf address : conf.getUserConf().getAddresses()) {
                    for (Map.Entry<String, String> entry : address.asMap().entrySet()) {
                        if (schemaEquals(Resource.User, "addresses." + entry.getKey(), schema)) {
                            attributeCond = new AttributeCond();
                            attributeCond.setSchema(entry.getValue());
                        }
                    }
                }
            }

            if (conf.getEnterpriseUserConf() != null) {
                for (Map.Entry<String, String> entry : conf.getEnterpriseUserConf().asMap().entrySet()) {
                    if (schemaEquals(Resource.EnterpriseUser, entry.getKey(), schema)) {
                        attributeCond = new AttributeCond();
                        attributeCond.setSchema(entry.getValue());
                    }
                }

                if (conf.getEnterpriseUserConf().getManager() != null
                        && conf.getEnterpriseUserConf().getManager().getManager() != null) {

                    attributeCond = new AttributeCond();
                    attributeCond.setSchema(conf.getEnterpriseUserConf().getManager().getManager());
                }
            }
        }

        if (attributeCond == null) {
            throw new IllegalArgumentException("Could not match " + schema + " for " + resource);
        }

        return attributeCond;
    }

    private SearchCond setOperator(final AttributeCond attributeCond, final String operator) {
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

    private <E extends Enum<?>> SearchCond complex(
            final String operator, final String left, final String right, final List<SCIMComplexConf<E>> items) {

        if (left.endsWith(".type")) {
            SCIMComplexConf<E> item = IterableUtils.find(items, new Predicate<SCIMComplexConf<E>>() {

                @Override
                public boolean evaluate(final SCIMComplexConf<E> object) {
                    return object.getType().name().equals(StringUtils.strip(right, "\""));
                }
            });
            if (item != null) {
                AttributeCond attributeCond = new AttributeCond();
                attributeCond.setSchema(item.getValue());
                attributeCond.setType(AttributeCond.Type.ISNOTNULL);
                return SearchCond.getLeafCond(attributeCond);
            }
        } else if (!conf.getUserConf().getEmails().isEmpty()
                && (MULTIVALUE.contains(left) || left.endsWith(".value"))) {

            List<SearchCond> orConds = new ArrayList<>();
            for (SCIMComplexConf<E> item : items) {
                AttributeCond cond = new AttributeCond();
                cond.setSchema(item.getValue());
                cond.setExpression(StringUtils.strip(right, "\""));
                orConds.add(setOperator(cond, operator));
            }
            if (!orConds.isEmpty()) {
                return SearchCond.getOrCond(orConds);
            }
        }

        return null;
    }

    private SearchCond addresses(
            final String operator, final String left, final String right, final List<SCIMUserAddressConf> items) {

        if (left.endsWith(".type") && "eq".equals(operator)) {
            SCIMUserAddressConf item = IterableUtils.find(items, new Predicate<SCIMUserAddressConf>() {

                @Override
                public boolean evaluate(final SCIMUserAddressConf object) {
                    return object.getType().name().equals(StringUtils.strip(right, "\""));
                }
            });
            if (item != null) {
                AttributeCond attributeCond = new AttributeCond();
                attributeCond.setSchema(item.getFormatted());
                attributeCond.setType(AttributeCond.Type.ISNOTNULL);
                return SearchCond.getLeafCond(attributeCond);
            }
        } else if (!conf.getUserConf().getEmails().isEmpty()
                && (MULTIVALUE.contains(left) || left.endsWith(".value"))) {

            List<SearchCond> orConds = new ArrayList<>();
            for (SCIMUserAddressConf item : items) {
                AttributeCond cond = new AttributeCond();
                cond.setSchema(item.getFormatted());
                cond.setExpression(StringUtils.strip(right, "\""));
                orConds.add(setOperator(cond, operator));
            }
            if (!orConds.isEmpty()) {
                return SearchCond.getOrCond(orConds);
            }
        }

        return null;
    }

    private SearchCond transform(final String operator, final String left, final String right) {
        SearchCond result = null;

        if (MULTIVALUE.contains(StringUtils.substringBefore(left, "."))) {
            if (conf.getUserConf() == null) {
                throw new IllegalArgumentException("No " + SCIMUserConf.class.getName() + " provided, cannot continue");
            }

            switch (StringUtils.substringBefore(left, ".")) {
                case "emails":
                    result = complex(operator, left, right, conf.getUserConf().getEmails());
                    break;

                case "phoneNumbers":
                    result = complex(operator, left, right, conf.getUserConf().getPhoneNumbers());
                    break;

                case "ims":
                    result = complex(operator, left, right, conf.getUserConf().getIms());
                    break;

                case "photos":
                    result = complex(operator, left, right, conf.getUserConf().getPhotos());
                    break;

                case "addresses":
                    result = addresses(operator, left, right, conf.getUserConf().getAddresses());
                    break;

                default:
            }
        }

        if (result == null) {
            AttributeCond attributeCond = createAttributeCond(left);
            attributeCond.setExpression(StringUtils.strip(right, "\""));
            result = setOperator(attributeCond, operator);
        }

        if (result == null) {
            throw new IllegalArgumentException(
                    "Could not handle (" + left + " " + operator + " " + right + ") for " + resource);
        }
        return result;
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
