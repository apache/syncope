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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.DefaultInboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRuleConfClass;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;

@InboundCorrelationRuleConfClass(DefaultInboundCorrelationRuleConf.class)
public class DefaultInboundCorrelationRule implements InboundCorrelationRule {

    private DefaultInboundCorrelationRuleConf conf;

    @Override
    public void setConf(final InboundCorrelationRuleConf conf) {
        if (conf instanceof DefaultInboundCorrelationRuleConf) {
            this.conf = DefaultInboundCorrelationRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    DefaultInboundCorrelationRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    @Override
    public SearchCond getSearchCond(final LiveSyncDelta syncDelta, final Provision provision) {
        Map<String, Item> mappingItems = provision.getMapping().getItems().stream().
                collect(Collectors.toMap(Item::getIntAttrName, Function.identity()));

        // search for anys by attribute(s) specified in the policy
        List<SearchCond> searchConds = new ArrayList<>();

        conf.getSchemas().forEach(schema -> {
            Attribute attr = Optional.ofNullable(mappingItems.get(schema)).
                    map(item -> syncDelta.getObject().getAttributeByName(item.getExtAttrName())).orElse(null);
            if (attr == null) {
                throw new IllegalArgumentException(
                        "Connector object does not contains the attributes to perform the search: " + schema);
            }

            AttrCond.Type type;
            String expression = null;

            if (attr.getValue() == null || attr.getValue().isEmpty()
                    || (attr.getValue().size() == 1 && attr.getValue().getFirst() == null)) {

                type = AttrCond.Type.ISNULL;
            } else {
                type = AttrCond.Type.EQ;
                expression = attr.getValue().size() > 1
                        ? attr.getValue().toString()
                        : attr.getValue().getFirst().toString();
            }

            AttrCond cond = "key".equalsIgnoreCase(schema)
                    || "username".equalsIgnoreCase(schema) || "name".equalsIgnoreCase(schema)
                    ? new AnyCond()
                    : new AttrCond();
            cond.setSchema(schema);
            cond.setType(type);
            cond.setExpression(expression);

            searchConds.add(SearchCond.of(cond));
        });

        return conf.isOrSchemas()
                ? SearchCond.or(searchConds)
                : SearchCond.and(searchConds);
    }
}
