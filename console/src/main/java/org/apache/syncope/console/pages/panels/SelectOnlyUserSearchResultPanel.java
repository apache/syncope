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
import java.util.List;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.console.pages.DisplayAttributesModalPage;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class SelectOnlyUserSearchResultPanel extends UserSearchResultPanel {

    private static final long serialVersionUID = 2146781496050131930L;

    private final PageReference pageRef;

    private final ModalWindow window;

    public <T extends AbstractAttributableTO> SelectOnlyUserSearchResultPanel(final String id, final boolean filtered,
            final NodeCond searchCond, final PageReference pageRef, final ModalWindow window,
            final UserRestClient restClient) {

        super(id, filtered, searchCond, pageRef, restClient);

        this.pageRef = pageRef;
        this.window = window;
    }

    @Override
    protected List<IColumn<AbstractAttributableTO, String>> getColumns() {
        final List<IColumn<AbstractAttributableTO, String>> columns =
                new ArrayList<IColumn<AbstractAttributableTO, String>>();
        for (String name : DisplayAttributesModalPage.DEFAULT_SELECTION) {
            columns.add(new PropertyColumn<AbstractAttributableTO, String>(new ResourceModel(name, name), name, name));
        }

        columns.add(new AbstractColumn<AbstractAttributableTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 8263694778917279290L;

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractAttributableTO>> cellItem,
                    final String componentId, final IModel<AbstractAttributableTO> rowModel) {

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, rowModel, pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        send(pageRef.getPage(), Broadcast.BREADTH, new RoleDetailsPanel.UserOwnerSelectPayload(
                                rowModel.getObject().getId()));
                        window.close(target);
                    }
                }, ActionLink.ActionType.SELECT, "Users");

                cellItem.add(panel);
            }
        });

        return columns;
    }
}
