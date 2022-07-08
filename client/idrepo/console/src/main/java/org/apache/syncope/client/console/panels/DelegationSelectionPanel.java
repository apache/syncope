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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class DelegationSelectionPanel extends Panel {

    private static final long serialVersionUID = 5820439948762L;

    public DelegationSelectionPanel(final String id, final String delegating) {
        super(id);

        IndicatingOnConfirmAjaxLink<String> link =
                new IndicatingOnConfirmAjaxLink<>("link", "confirmDelegation", true) {

            private static final long serialVersionUID = 6611857585742411796L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SyncopeConsoleSession.get().setDelegatedBy(delegating);
                setResponsePage(Dashboard.class);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                if (delegating.equals(SyncopeConsoleSession.get().getDelegatedBy())) {
                    tag.append("class", "disabled", " ");
                }
            }
        };
        add(link);
        link.setOutputMarkupId(true);
        link.add(new Label("label", delegating));
    }
}
