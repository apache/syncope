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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRuleConfClass;

@PullCorrelationRuleConfClass(DefaultPullCorrelationRuleConf.class)
public class DefaultPullCorrelationRule implements PullCorrelationRule {

    private DefaultPullCorrelationRuleConf conf;

    @Override
    public void setConf(final PullCorrelationRuleConf conf) {
        if (conf instanceof DefaultPullCorrelationRuleConf) {
            this.conf = DefaultPullCorrelationRuleConf.class.cast(conf);
        } else {
            throw new IllegalArgumentException(
                    DefaultPullCorrelationRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    @Override
    public SearchCond getSearchCond(final ConnectorObject connObj, final Provision provision) {
        Map<String, Item> mappingItems = provision.getMapping().getItems().stream().
                collect(Collectors.toMap(Item::getIntAttrName, Function.identity()));

        // search for anys by attribute(s) specified in the policy
        SearchCond searchCond = null;

        for (String schema : conf.getSchemas()) {
            Item mappingItem = mappingItems.get(schema);
            Attribute attr = mappingItem == null
                    ? null
                    : connObj.getAttributeByName(mappingItem.getExtAttrName());
            if (attr == null) {
                throw new IllegalArgumentException(
                        "Connector object does not contains the attributes to perform the search: " + schema);
            }

            AttributeCond.Type type;
            String expression = null;

            if (attr.getValue() == null || attr.getValue().isEmpty()
                    || (attr.getValue().size() == 1 && attr.getValue().get(0) == null)) {

                type = AttributeCond.Type.ISNULL;
            } else {
                type = AttributeCond.Type.EQ;
                expression = attr.getValue().size() > 1
                        ? attr.getValue().toString()
                        : attr.getValue().get(0).toString();
            }

            SearchCond nodeCond;
            // users: just key or username can be selected
            // groups: just key or name can be selected
            // any objects: just key or name can be selected
            if ("key".equalsIgnoreCase(schema)
                    || "username".equalsIgnoreCase(schema) || "name".equalsIgnoreCase(schema)) {

                AnyCond cond = new AnyCond();
                cond.setSchema(schema);
                cond.setType(type);
                cond.setExpression(expression);

                nodeCond = SearchCond.getLeafCond(cond);
            } else {
                AttributeCond cond = new AttributeCond();
                cond.setSchema(schema);
                cond.setType(type);
                cond.setExpression(expression);

                nodeCond = SearchCond.getLeafCond(cond);
            }

            searchCond = searchCond == null
                    ? nodeCond
                    : SearchCond.getAndCond(searchCond, nodeCond);
        }

        return searchCond;
    }

}
