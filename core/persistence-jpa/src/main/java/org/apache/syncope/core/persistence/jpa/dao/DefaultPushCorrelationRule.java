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
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.Attribute;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.AccountGetter;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PlainAttrGetter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;

@PushCorrelationRuleConfClass(DefaultPushCorrelationRuleConf.class)
public class DefaultPushCorrelationRule implements PushCorrelationRule {

    @Autowired
    private MappingManager mappingManager;

    private DefaultPushCorrelationRuleConf conf;

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
                item -> item.getPurpose() == MappingPurpose.PROPAGATION || item.getPurpose() == MappingPurpose.BOTH).
                forEach(item -> {
                    Pair<String, Attribute> attr = mappingManager.prepareAttr(
                            provision,
                            item,
                            any,
                            null,
                            AccountGetter.DEFAULT,
                            AccountGetter.DEFAULT,
                            PlainAttrGetter.DEFAULT);
                    if (attr != null && attr.getRight() != null && conf.getSchemas().contains(item.getIntAttrName())) {
                        filters.add(provision.isIgnoreCaseMatch()
                                ? FilterBuilder.equalsIgnoreCase(attr.getRight())
                                : FilterBuilder.equalTo(attr.getRight()));
                    }
                });

        return conf.isOrSchemas()
                ? FilterBuilder.or(filters)
                : FilterBuilder.and(filters);
    }
}
