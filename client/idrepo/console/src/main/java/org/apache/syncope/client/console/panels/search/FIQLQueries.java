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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.FIQLQueryRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;

public class FIQLQueries extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -6210866598876608258L;

    public FIQLQueries(
            final String id,
            final FIQLQueryRestClient fiqlQueryRestClient,
            final AbstractSearchPanel searchPanel,
            final String target,
            final PageReference pageRef) {

        super(id, pageRef);

        addInnerObject(new FIQLQueryDirectoryPanel(
                "fiqlQueryDirectoryPanel", fiqlQueryRestClient, searchPanel, target, this, pageRef));
    }

    @Override
    protected String getTargetKey(final Serializable modelObject) {
        return getString("fiqlQueries");
    }

    @Override
    public void toggle(final AjaxRequestTarget target, final boolean toggle) {
        if (toggle) {
            setHeader(target, getString("fiqlQueries"));
        }
        super.toggle(target, toggle);
    }
}
