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

import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class WAConfigDirectoryPanel extends AttrListDirectoryPanel {

    private static final long serialVersionUID = 1538796157345L;

    public WAConfigDirectoryPanel(final String id, final WAConfigRestClient restClient, final PageReference pageRef) {
        super(id, restClient, pageRef, true);

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<Attr>(new Attr(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<Attr> build(final String id, final int index, final AjaxWizard.Mode mode) {
                return new WAConfigModalPanel(modal, newModelObject(), mode, pageRef);
            }
        }, true);

        initResultTable();
    }

    @Override
    protected WAConfigProvider dataProvider() {
        return new WAConfigProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_WACONFIG_PAGINATOR_ROWS;
    }

    @Override
    public ActionsPanel<Attr> getActions(final IModel<Attr> model) {
        ActionsPanel<Attr> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                target.add(modal);
                modal.header(new StringResourceModel("any.edit"));
                modal.setContent(new WAConfigModalPanel(modal, model.getObject(), AjaxWizard.Mode.EDIT, pageRef));
                modal.show(true);
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.WA_CONFIG_SET);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                try {
                    ((WAConfigRestClient) restClient).delete(model.getObject().getSchema());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getSchema(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.WA_CONFIG_DELETE, true);

        return panel;
    }

    protected final class WAConfigProvider extends AttrListProvider {

        private static final long serialVersionUID = -185944053385660794L;

        private WAConfigProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        protected List<Attr> list() {
            return ((WAConfigRestClient) restClient).list();
        }
    }
}
