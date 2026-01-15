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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;

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
        if (uDynClauses == null) {
            uDynClauses = SearchUtils.getSearchClauses(anyTO.getDynMembershipConds().get(AnyTypeKind.USER.name()));
        }
        return uDynClauses;
    }

    public void setUDynClauses(final List<SearchClause> uDynClauses) {
        this.uDynClauses = uDynClauses;
    }

    public final Map<String, List<SearchClause>> getADynClauses() {
        if (aDynClauses == null) {
            aDynClauses = SearchUtils.getSearchClauses(anyTO.getDynMembershipConds().entrySet().stream().
                    filter(e -> !e.getKey().equals(AnyTypeKind.USER.name())).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return aDynClauses;
    }

    public void setADynClauses(final Map<String, List<SearchClause>> aDynClauses) {
        this.aDynClauses = aDynClauses;
    }

    public String getUDynMembershipCond() {
        return CollectionUtils.isEmpty(uDynClauses)
                ? null
                : SearchUtils.buildFIQL(uDynClauses, SyncopeClient.getUserSearchConditionBuilder());
    }

    public Map<String, String> getADynMembershipConds() {
        Map<String, String> res = new HashMap<>();
        if (aDynClauses != null) {
            aDynClauses.entrySet().stream().
                    filter(e -> CollectionUtils.isNotEmpty(e.getValue())).
                    forEach(e -> {
                        String fiql = SearchUtils.buildFIQL(e.getValue(),
                                SyncopeClient.getAnyObjectSearchConditionBuilder(e.getKey()));
                        Optional.ofNullable(fiql).ifPresent(f -> res.put(e.getKey(), f));
                    });
        }

        return res;
    }

    public GroupTO fillDynamicConditions() {
        anyTO.getDynMembershipConds().clear();
        Optional.ofNullable(getUDynMembershipCond()).
                ifPresent(cond -> anyTO.getDynMembershipConds().put(AnyTypeKind.USER.name(), cond));
        anyTO.getDynMembershipConds().putAll(getADynMembershipConds());
        return anyTO;
    }
}
