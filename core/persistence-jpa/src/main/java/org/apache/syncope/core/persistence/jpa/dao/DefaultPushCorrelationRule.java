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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.search.ConnObjectTOFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.Attribute;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.AccountGetter;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PlainAttrGetter;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

@PushCorrelationRuleConfClass(DefaultPushCorrelationRuleConf.class)
public class DefaultPushCorrelationRule implements PushCorrelationRule {

    protected static final ConnObjectTOFiqlSearchConditionBuilder FIQL_BUILDER =
            new ConnObjectTOFiqlSearchConditionBuilder();

    @Autowired
    protected MappingManager mappingManager;

    protected DefaultPushCorrelationRuleConf conf;

    @Override
    public void setConf(final PushCorrelationRuleConf conf) {
        if (conf instanceof DefaultPushCorrelationRuleConf) {
            this.conf = DefaultPushCorrelationRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    DefaultPushCorrelationRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    @Override
    public Filter getFilter(final Any<?> any, final Provision provision) {
        List<Filter> filters = new ArrayList<>();

        provision.getMapping().getItems().stream().filter(
                item -> conf.getSchemas().contains(item.getIntAttrName()) && item.getPurpose() != MappingPurpose.NONE).
                forEach(item -> {
                    Pair<String, Attribute> attr = mappingManager.prepareAttr(
                            provision,
                            item,
                            any,
                            null,
                            AccountGetter.DEFAULT,
                            AccountGetter.DEFAULT,
                            PlainAttrGetter.DEFAULT);
                    if (attr != null) {
                        Attribute toFilter = null;
                        if (attr.getLeft() != null) {
                            toFilter = AttributeBuilder.build(item.getExtAttrName(), attr.getLeft());
                        } else if (attr.getRight() != null) {
                            toFilter = attr.getRight();
                        }
                        if (toFilter != null) {
                            filters.add(provision.isIgnoreCaseMatch()
                                    ? FilterBuilder.equalsIgnoreCase(toFilter)
                                    : FilterBuilder.equalTo(toFilter));
                        }
                    }
                });

        return conf.isOrSchemas()
                ? FilterBuilder.or(filters)
                : FilterBuilder.and(filters);
    }

    @Override
    public String getFiql(final ConnectorObject connectorObject, final Provision provision) {
        List<CompleteCondition> conditions = new ArrayList<>();

        provision.getMapping().getItems().stream().filter(
                item -> conf.getSchemas().contains(item.getIntAttrName()) && item.getPurpose() != MappingPurpose.NONE).
                forEach(item -> Optional.ofNullable(connectorObject.getAttributeByName(item.getExtAttrName())).
                ifPresent(attr -> {
                    if (CollectionUtils.isEmpty(attr.getValue())) {
                        conditions.add(FIQL_BUILDER.isNull(attr.getName()));
                    } else {
                        List<CompleteCondition> valueConditions = new ArrayList<>();

                        attr.getValue().stream().filter(Objects::nonNull).forEach(value -> {
                            if (value instanceof GuardedString) {
                                valueConditions.add(FIQL_BUILDER.is(attr.getName()).
                                        equalTo(SecurityUtil.decrypt((GuardedString) value)));
                            } else if (value instanceof GuardedByteArray) {
                                valueConditions.add(FIQL_BUILDER.is(attr.getName()).
                                        equalTo(new String(SecurityUtil.decrypt((GuardedByteArray) value))));
                            } else if (value instanceof byte[]) {
                                valueConditions.add(FIQL_BUILDER.is(attr.getName()).
                                        equalTo(Base64.getEncoder().encodeToString((byte[]) value)));
                            } else {
                                valueConditions.add(FIQL_BUILDER.is(attr.getName()).equalTo(value.toString()));
                            }
                        });

                        if (!valueConditions.isEmpty()) {
                            conditions.add(valueConditions.size() == 1
                                    ? valueConditions.get(0)
                                    : FIQL_BUILDER.and(valueConditions));
                        }
                    }
                }));

        return conf.isOrSchemas()
                ? FIQL_BUILDER.or(conditions).query()
                : FIQL_BUILDER.and(conditions).query();
    }
}
