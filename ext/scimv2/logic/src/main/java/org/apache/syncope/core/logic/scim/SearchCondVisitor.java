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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserAddressConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.type.Resource;

/**
 * Visits SCIM filter expression and produces {@link SearchCond}.
 */
public class SearchCondVisitor extends SCIMFilterBaseVisitor<SearchCond> {

    private static final List<String> MULTIVALUE = List.of(
            "emails", "phoneNumbers", "ims", "photos", "addresses");

    private static boolean schemaEquals(final Resource resource, final String value, final String schema) {
        return resource == null
                ? value.contains(":")
                ? StringUtils.substringAfterLast(value, ":").equalsIgnoreCase(schema)
                : value.equalsIgnoreCase(schema)
                : value.equalsIgnoreCase(schema) || (resource.schema() + ":" + value).equalsIgnoreCase(schema);
    }

    private static SearchCond setOperator(final AttrCond attrCond, final String operator) {
        switch (operator) {
            case "eq":
            default:
                attrCond.setType(AttrCond.Type.IEQ);
                break;

            case "ne":
                attrCond.setType(AttrCond.Type.IEQ);
                break;

            case "sw":
                attrCond.setType(AttrCond.Type.ILIKE);
                attrCond.setExpression(attrCond.getExpression() + "%");
                break;

            case "co":
                attrCond.setType(AttrCond.Type.ILIKE);
                attrCond.setExpression("%" + attrCond.getExpression() + "%");
                break;

            case "ew":
                attrCond.setType(AttrCond.Type.ILIKE);
                attrCond.setExpression("%" + attrCond.getExpression());
                break;

            case "gt":
                attrCond.setType(AttrCond.Type.GT);
                break;

            case "ge":
                attrCond.setType(AttrCond.Type.GE);
                break;

            case "lt":
                attrCond.setType(AttrCond.Type.LT);
                break;

            case "le":
                attrCond.setType(AttrCond.Type.LE);
                break;
        }

        return "ne".equals(operator)
                ? SearchCond.negate(attrCond)
                : SearchCond.of(attrCond);
    }

    private static <E extends Enum<?>> SearchCond complex(
            final String operator, final String left, final String right, final List<SCIMComplexConf<E>> items) {

        if (left.endsWith(".type") && "eq".equals(operator)) {
            Optional<SCIMComplexConf<E>> item = items.stream().
                    filter(object -> object.getType().name().equals(StringUtils.strip(right, "\""))).findFirst();
            if (item.isPresent()) {
                AttrCond attrCond = new AttrCond();
                attrCond.setSchema(item.get().getValue());
                attrCond.setType(AttrCond.Type.ISNOTNULL);
                return SearchCond.of(attrCond);
            }
        } else if (MULTIVALUE.contains(left) || left.endsWith(".value")) {
            List<SearchCond> orConds = items.stream().
                    filter(item -> item.getValue() != null).
                    map(item -> {
                        AttrCond cond = new AttrCond();
                        cond.setSchema(item.getValue());
                        cond.setExpression(StringUtils.strip(right, "\""));
                        return setOperator(cond, operator);
                    }).collect(Collectors.toList());
            if (!orConds.isEmpty()) {
                return SearchCond.or(orConds);
            }
        }

        return null;
    }

    private static SearchCond addresses(
            final String operator, final String left, final String right, final List<SCIMUserAddressConf> items) {

        if (left.endsWith(".type") && "eq".equals(operator)) {
            Optional<SCIMUserAddressConf> item = items.stream().
                    filter(object -> object.getType().name().equals(StringUtils.strip(right, "\""))).findFirst();
            if (item.isPresent()) {
                AttrCond attrCond = new AttrCond();
                attrCond.setSchema(item.get().getFormatted());
                attrCond.setType(AttrCond.Type.ISNOTNULL);
                return SearchCond.of(attrCond);
            }
        } else if (MULTIVALUE.contains(left) || left.endsWith(".value")) {
            List<SearchCond> orConds = items.stream().
                    filter(item -> item.getFormatted() != null).
                    map(item -> {
                        AttrCond cond = new AttrCond();
                        cond.setSchema(item.getFormatted());
                        cond.setExpression(StringUtils.strip(right, "\""));
                        return setOperator(cond, operator);
                    }).collect(Collectors.toList());
            if (!orConds.isEmpty()) {
                return SearchCond.or(orConds);
            }
        }

        return null;
    }

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

    public AttrCond createAttrCond(final String schema) {
        AttrCond attrCond = null;

        if (schemaEquals(Resource.User, "userName", schema)) {
            attrCond = new AnyCond();
            attrCond.setSchema("username");
        } else if (resource == Resource.Group && schemaEquals(Resource.Group, "displayName", schema)) {
            attrCond = new AnyCond();
            attrCond.setSchema("name");
        } else if (schemaEquals(null, "meta.created", schema)) {
            attrCond = new AnyCond();
            attrCond.setSchema("creationDate");
        } else if (schemaEquals(null, "meta.lastModified", schema)) {
            attrCond = new AnyCond();
            attrCond.setSchema("lastChangeDate");
        }

        switch (resource) {
            case User:
                if (conf.getUserConf() != null) {
                    if (conf.getUserConf().getName() != null) {
                        for (Map.Entry<String, String> entry : conf.getUserConf().getName().asMap().entrySet()) {
                            if (schemaEquals(Resource.User, "name." + entry.getKey(), schema)) {
                                attrCond = new AttrCond();
                                attrCond.setSchema(entry.getValue());
                            }
                        }
                    }

                    for (Map.Entry<String, String> entry : conf.getUserConf().asMap().entrySet()) {
                        if (schemaEquals(Resource.User, entry.getKey(), schema)) {
                            attrCond = new AttrCond();
                            attrCond.setSchema(entry.getValue());
                        }
                    }

                    for (SCIMUserAddressConf address : conf.getUserConf().getAddresses()) {
                        for (Map.Entry<String, String> entry : address.asMap().entrySet()) {
                            if (schemaEquals(Resource.User, "addresses." + entry.getKey(), schema)) {
                                attrCond = new AttrCond();
                                attrCond.setSchema(entry.getValue());
                            }
                        }
                    }
                }

                if (conf.getEnterpriseUserConf() != null) {
                    for (Map.Entry<String, String> entry : conf.getEnterpriseUserConf().asMap().entrySet()) {
                        if (schemaEquals(Resource.EnterpriseUser, entry.getKey(), schema)) {
                            attrCond = new AttrCond();
                            attrCond.setSchema(entry.getValue());
                        }
                    }

                    if (conf.getEnterpriseUserConf().getManager() != null
                            && conf.getEnterpriseUserConf().getManager().getKey() != null) {

                        attrCond = new AttrCond();
                        attrCond.setSchema(conf.getEnterpriseUserConf().getManager().getKey());
                    }
                }

                if (conf.getExtensionUserConf() != null) {
                    for (Map.Entry<String, String> entry : conf.getExtensionUserConf().asMap().entrySet()) {
                        if (schemaEquals(Resource.ExtensionUser, entry.getKey(), schema)) {
                            attrCond = new AttrCond();
                            attrCond.setSchema(entry.getValue());
                        }
                    }
                }
                break;

            case Group:
                if (conf.getGroupConf() != null) {
                    for (Map.Entry<String, String> entry : conf.getGroupConf().asMap().entrySet()) {
                        if (schemaEquals(Resource.Group, entry.getKey(), schema)) {
                            attrCond = new AttrCond();
                            attrCond.setSchema(entry.getValue());
                        }
                    }
                }
                break;

            default:
        }

        if (attrCond == null) {
            throw new IllegalArgumentException("Could not match " + schema + " for " + resource);
        }

        return attrCond;
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
            AttrCond attrCond = createAttrCond(left);
            attrCond.setExpression(StringUtils.strip(right, "\""));
            result = setOperator(attrCond, operator);
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
        AttrCond cond = createAttrCond(ctx.ATTRNAME().getText());
        cond.setType(AttrCond.Type.ISNOTNULL);
        return SearchCond.of(cond);
    }

    @Override
    public SearchCond visitLPAREN_EXPR_RPAREN(final SCIMFilterParser.LPAREN_EXPR_RPARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public SearchCond visitNOT_EXPR(final SCIMFilterParser.NOT_EXPRContext ctx) {
        SearchCond cond = visit(ctx.expression());
        Optional<AnyCond> anyCond = cond.asLeaf(AnyCond.class);
        if (anyCond.isPresent()) {
            if (anyCond.get().getType() == AttrCond.Type.ISNULL) {
                anyCond.get().setType(AttrCond.Type.ISNOTNULL);
            } else if (anyCond.get().getType() == AttrCond.Type.ISNOTNULL) {
                anyCond.get().setType(AttrCond.Type.ISNULL);
            }
        } else {
            Optional<AttrCond> attrCond = cond.asLeaf(AttrCond.class);
            if (attrCond.isPresent()) {
                if (attrCond.get().getType() == AnyCond.Type.ISNULL) {
                    attrCond.get().setType(AnyCond.Type.ISNOTNULL);
                } else if (attrCond.get().getType() == AnyCond.Type.ISNOTNULL) {
                    attrCond.get().setType(AnyCond.Type.ISNULL);
                }
            } else {
                cond = SearchCond.negate(cond);
            }
        }

        return cond;
    }

    @Override
    public SearchCond visitEXPR_AND_EXPR(final SCIMFilterParser.EXPR_AND_EXPRContext ctx) {
        return SearchCond.and(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }

    @Override
    public SearchCond visitEXPR_OR_EXPR(final SCIMFilterParser.EXPR_OR_EXPRContext ctx) {
        return SearchCond.or(visit(ctx.expression(0)), visit(ctx.expression(1)));
    }
}
