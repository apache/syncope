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
package org.apache.syncope.client.console.wizards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public class DynRealmWrapper extends EntityWrapper<DynRealmTO> {

    private static final long serialVersionUID = 7226128615964284614L;

    private Map<String, List<SearchClause>> dynClauses;

    public DynRealmWrapper(final DynRealmTO dynRealmTO) {
        super(dynRealmTO);
        getDynClauses();
    }

    public final Map<String, List<SearchClause>> getDynClauses() {
        if (this.dynClauses == null) {
            this.dynClauses = SearchUtils.getSearchClauses(getInnerObject().getDynMembershipConds());
        }
        return this.dynClauses;
    }

    public void setDynClauses(final Map<String, List<SearchClause>> dynClauses) {
        this.dynClauses.clear();
        this.dynClauses.putAll(dynClauses);
    }

    public Map<String, String> getDynMembershipConds() {
        final Map<String, String> res = new HashMap<>();
        if (this.dynClauses != null && !this.dynClauses.isEmpty()) {
            this.dynClauses.entrySet().stream().
                    filter(entry -> (CollectionUtils.isNotEmpty(entry.getValue()))).
                    forEachOrdered(entry -> {
                        AbstractFiqlSearchConditionBuilder<?, ?, ?> builder =
                                AnyTypeKind.USER.name().equals(entry.getKey())
                                ? SyncopeClient.getUserSearchConditionBuilder()
                                : AnyTypeKind.GROUP.name().equals(entry.getKey())
                                ? SyncopeClient.getGroupSearchConditionBuilder()
                                : SyncopeClient.getAnyObjectSearchConditionBuilder(entry.getKey());
                        String fiql = SearchUtils.buildFIQL(entry.getValue(), builder);
                        if (fiql != null) {
                            res.put(entry.getKey(), fiql);
                        }
                    });
        }

        return res;
    }

    public DynRealmTO fillDynamicConditions() {
        getInnerObject().getDynMembershipConds().clear();
        getInnerObject().getDynMembershipConds().putAll(this.getDynMembershipConds());
        return getInnerObject();
    }
}
