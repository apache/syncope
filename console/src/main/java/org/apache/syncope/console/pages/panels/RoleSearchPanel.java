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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.commons.SearchCondWrapper;
import org.apache.wicket.model.LoadableDetachableModel;

public class RoleSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 5757183539269316263L;

    public RoleSearchPanel(final String id) {
        this(id, null, true);
    }

    public RoleSearchPanel(final String id, final NodeCond initCond) {
        this(id, initCond, true);
    }

    public RoleSearchPanel(final String id, final NodeCond initCond, final boolean required) {
        super(id, AttributableType.ROLE, initCond, required);
    }

    @Override
    protected void populate() {
        super.populate();

        this.filterTypes = new LoadableDetachableModel<List<SearchCondWrapper.FilterType>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchCondWrapper.FilterType> load() {
                List<SearchCondWrapper.FilterType> result = new ArrayList<SearchCondWrapper.FilterType>();
                result.add(SearchCondWrapper.FilterType.ATTRIBUTE);
                result.add(SearchCondWrapper.FilterType.ENTITLEMENT);
                result.add(SearchCondWrapper.FilterType.RESOURCE);
                return result;
            }
        };

        this.roleNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return Collections.<String>emptyList();
            }
        };
    }
}
