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
package org.apache.syncope.client.console.wizards.any;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.GroupTO;

public class GroupWrapper extends AnyWrapper<GroupTO> {

    private static final long serialVersionUID = 8058288034211558376L;

    private GroupTO previousGroupTO;

    private List<SearchClause> uDynClauses;

    private Map<String, List<SearchClause>> aDynClauses;

    public GroupWrapper(final GroupTO groupTO) {
        this(null, groupTO);
    }

    public GroupWrapper(final GroupTO previousGroupTO, final GroupTO groupTO) {
        super(groupTO);
        this.previousGroupTO = previousGroupTO;
        getUDynClauses();
        getADynClauses();
    }

    public GroupTO getPreviousGroupTO() {
        return previousGroupTO;
    }

    public final List<SearchClause> getUDynClauses() {
        if (this.uDynClauses == null) {
            this.uDynClauses = SearchUtils.getSearchClauses(this.anyTO.getUDynMembershipCond());
        }
        return this.uDynClauses;
    }

    public void setUDynClauses(final List<SearchClause> uDynClauses) {
        this.uDynClauses = uDynClauses;
    }

    public final Map<String, List<SearchClause>> getADynClauses() {
        if (this.aDynClauses == null) {
            this.aDynClauses = SearchUtils.getSearchClauses(this.anyTO.getADynMembershipConds());
        }
        return this.aDynClauses;
    }

    public void setADynClauses(final Map<String, List<SearchClause>> aDynClauses) {
        this.aDynClauses = aDynClauses;
    }

    public String getUDynMembershipCond() {
        return CollectionUtils.isEmpty(this.uDynClauses)
                ? null
                : SearchUtils.buildFIQL(this.uDynClauses, SyncopeClient.getUserSearchConditionBuilder());
    }

    public Map<String, String> getADynMembershipConds() {
        Map<String, String> res = new HashMap<>();
        if (this.aDynClauses != null && !this.aDynClauses.isEmpty()) {
            this.aDynClauses.entrySet().stream().
                    filter(entry -> CollectionUtils.isNotEmpty(entry.getValue())).
                    forEach(entry -> {
                        String fiql = SearchUtils.buildFIQL(entry.getValue(),
                                SyncopeClient.getAnyObjectSearchConditionBuilder(entry.getKey()));
                        if (fiql != null) {
                            res.put(entry.getKey(), fiql);
                        }
                    });
        }

        return res;
    }

    public GroupTO fillDynamicConditions() {
        this.anyTO.setUDynMembershipCond(this.getUDynMembershipCond());
        this.anyTO.getADynMembershipConds().clear();
        this.anyTO.getADynMembershipConds().putAll(this.getADynMembershipConds());
        return this.anyTO;
    }
}
