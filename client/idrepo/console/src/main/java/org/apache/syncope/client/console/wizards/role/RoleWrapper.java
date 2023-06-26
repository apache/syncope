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
package org.apache.syncope.client.console.wizards.role;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.to.RoleTO;

public class RoleWrapper extends EntityWrapper<RoleTO> {

    private static final long serialVersionUID = 8058288034211558376L;

    private List<SearchClause> dynClauses;

    public RoleWrapper(final RoleTO roleTO) {
        super(roleTO);
        getDynClauses();
    }

    public final List<SearchClause> getDynClauses() {
        if (this.dynClauses == null) {
            this.dynClauses = SearchUtils.getSearchClauses(getInnerObject().getDynMembershipCond());
        }
        return this.dynClauses;
    }

    public void setDynClauses(final List<SearchClause> dynClauses) {
        this.dynClauses = dynClauses;
    }

    public String getDynMembershipCond() {
        if (CollectionUtils.isEmpty(this.dynClauses)) {
            return null;
        }

        return SearchUtils.buildFIQL(this.dynClauses, SyncopeClient.getUserSearchConditionBuilder());
    }

    public RoleTO fillDynamicConditions() {
        getInnerObject().setDynMembershipCond(this.getDynMembershipCond());
        return getInnerObject();
    }
}
