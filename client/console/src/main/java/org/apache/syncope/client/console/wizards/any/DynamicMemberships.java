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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.MapOfListModel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class DynamicMemberships extends WizardStep {

    private static final long serialVersionUID = 855618618337931784L;

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    public DynamicMemberships(final GroupWrapper groupWrapper) {
        super();

        final LoadableDetachableModel<List<AnyTypeTO>> types = new LoadableDetachableModel<List<AnyTypeTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AnyTypeTO> load() {
                return CollectionUtils.select(anyTypeRestClient.list(), new Predicate<AnyTypeTO>() {

                    @Override
                    public boolean evaluate(final AnyTypeTO t) {
                        return AnyTypeKind.USER != t.getKind() && AnyTypeKind.GROUP != t.getKind();
                    }
                }, new ArrayList<AnyTypeTO>());
            }
        };

        // ------------------------
        // uDynMembershipCond
        // ------------------------
        add(new Accordion("uDynMembershipCond", Collections.<ITab>singletonList(
                new AbstractTab(new ResourceModel("uDynMembershipCond", "Dynamic USER Membership Conditions")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public Panel getPanel(final String panelId) {
                return new UserSearchPanel.Builder(new PropertyModel<List<SearchClause>>(groupWrapper, "uDynClauses")).
                        required(false).build(panelId);
            }
        }), Model.of(StringUtils.isBlank(groupWrapper.getUDynMembershipCond()) ? -1 : 0)).setOutputMarkupId(true));
        // ------------------------ 

        // ------------------------
        // aDynMembershipConds
        // ------------------------
        add(new ListView<AnyTypeTO>("aDynMembershipCond", types) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<AnyTypeTO> item) {
                final String key = item.getModelObject().getKey();
                item.add(new Accordion("aDynMembershipCond", Collections.<ITab>singletonList(
                        new AbstractTab(new StringResourceModel(
                                "aDynMembershipCond", this, new Model<AnyTypeTO>(item.getModelObject()))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public Panel getPanel(final String panelId) {
                        return new AnyObjectSearchPanel.Builder(
                                key, new MapOfListModel<SearchClause>(groupWrapper, "aDynClauses", key)).
                                required(false).build(panelId);
                    }
                }), Model.of(StringUtils.isBlank(groupWrapper.getADynMembershipConds().get(key)) ? -1 : 0))
                        .setOutputMarkupId(true));
            }
        });
        // ------------------------
    }
}
