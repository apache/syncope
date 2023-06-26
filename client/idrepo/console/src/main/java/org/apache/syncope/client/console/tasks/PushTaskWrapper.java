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
package org.apache.syncope.client.console.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.PushTaskTO;

public class PushTaskWrapper extends EntityWrapper<PushTaskTO> {

    private static final long serialVersionUID = 8058288034211558377L;

    private Map<String, List<SearchClause>> filterClauses;

    public PushTaskWrapper(final PushTaskTO pushTaskTO) {
        super(pushTaskTO);
        getFilterClauses();
    }

    public final Map<String, List<SearchClause>> getFilterClauses() {
        if (this.filterClauses == null) {
            this.filterClauses = SearchUtils.getSearchClauses(getInnerObject().getFilters());
        }
        return this.filterClauses;
    }

    public void setFilterClauses(final Map<String, List<SearchClause>> aDynClauses) {
        this.filterClauses = aDynClauses;
    }

    public Map<String, String> getFilters() {
        Map<String, String> filters = new HashMap<>();

        for (Map.Entry<String, List<SearchClause>> entry : getFilterClauses().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                AbstractFiqlSearchConditionBuilder<?, ?, ?> bld;
                switch (entry.getKey()) {
                    case "USER":
                        bld = SyncopeClient.getUserSearchConditionBuilder();
                        break;

                    case "GROUP":
                        bld = SyncopeClient.getGroupSearchConditionBuilder();
                        break;

                    default:
                        bld = SyncopeClient.getAnyObjectSearchConditionBuilder(entry.getKey());
                }

                filters.put(entry.getKey(), SearchUtils.buildFIQL(entry.getValue(), bld));
            }
        }

        return filters;
    }

    public PushTaskTO fillFilterConditions() {
        getInnerObject().getFilters().clear();
        getInnerObject().getFilters().putAll(this.getFilters());
        return getInnerObject();
    }
}
