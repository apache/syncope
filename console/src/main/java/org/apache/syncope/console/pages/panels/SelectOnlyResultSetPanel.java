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
import org.apache.syncope.console.pages.DisplayAttributesModalPage;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.AbstractAttributableTO;
import org.apache.syncope.to.UserTO;
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

public class SelectOnlyResultSetPanel extends ResultSetPanel {

    private static final long serialVersionUID = 2146781496050131930L;

    private final PageReference callerRef;

    private final ModalWindow window;

    public <T extends AbstractAttributableTO> SelectOnlyResultSetPanel(final String id, final boolean filtered,
            final NodeCond searchCond, final PageReference callerRef, final ModalWindow window) {

        super(id, filtered, searchCond, callerRef);

        this.callerRef = callerRef;
        this.window = window;

        container.get("reload").setEnabled(false);
        container.get("reload").setVisible(false);

        container.get("displayAttrsLink").setEnabled(false);
        container.get("displayAttrsLink").setVisible(false);
    }

    @Override
    protected List<IColumn<UserTO>> getColumns() {
        final List<IColumn<UserTO>> columns = new ArrayList<IColumn<UserTO>>();
        for (String name : DisplayAttributesModalPage.DEFAULT_SELECTION) {
            columns.add(new PropertyColumn<UserTO>(new ResourceModel(name, name), name, name));
        }

        columns.add(new AbstractColumn<UserTO>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 8263694778917279290L;

            @Override
            public void populateItem(final Item<ICellPopulator<UserTO>> cellItem, final String componentId,
                    final IModel<UserTO> rowModel) {

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, rowModel);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        send(callerRef.getPage(), Broadcast.BREADTH, new RoleDetailsPanel.UserOwnerSelectPayload(
                                rowModel.getObject().getId()));
                        window.close(target);
                    }
                }, ActionLink.ActionType.SELECT, "Users", "read");

                cellItem.add(panel);
            }
        });

        return columns;
    }
}
