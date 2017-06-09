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

import java.io.Serializable;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.DynRealmTO;

public class DynRealmWrapper implements Serializable {

    private static final long serialVersionUID = 7226128615964284614L;

    private final DynRealmTO dynRealmTO;

    private List<SearchClause> dynClauses;

    public DynRealmWrapper(final DynRealmTO dynRealmTO) {
        this.dynRealmTO = dynRealmTO;
        getDynClauses();
    }

    public final List<SearchClause> getDynClauses() {
        if (this.dynClauses == null) {
            this.dynClauses = SearchUtils.getSearchClauses(this.dynRealmTO.getCond());
        }
        return this.dynClauses;
    }

    public void setDynClauses(final List<SearchClause> dynClauses) {
        this.dynClauses = dynClauses;
    }

    public String getCond() {
        if (CollectionUtils.isEmpty(this.dynClauses)) {
            return null;
        } else {
            return SearchUtils.buildFIQL(this.dynClauses, SyncopeClient.getUserSearchConditionBuilder());
        }
    }

    public DynRealmTO fillDynamicConditions() {
        this.dynRealmTO.setCond(this.getCond());
        return this.dynRealmTO;
    }

    public DynRealmTO getInnerObject() {
        return this.dynRealmTO;
    }
}
