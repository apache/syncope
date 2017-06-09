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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.DynRealmWrapper;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class DynRealmModalPanel extends AbstractModalPanel<DynRealmWrapper> {

    private static final long serialVersionUID = -3773196441177699452L;

    private final DynRealmRestClient restClient = new DynRealmRestClient();

    private final DynRealmWrapper dynRealmWrapper;

    private final boolean create;

    public DynRealmModalPanel(
            final DynRealmWrapper dynRealmWrapper,
            final boolean create,
            final BaseModal<DynRealmWrapper> modal,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.dynRealmWrapper = dynRealmWrapper;
        this.create = create;
        modal.setFormModel(dynRealmWrapper);

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                "key", "key", new PropertyModel<String>(dynRealmWrapper.getInnerObject(), "key"), false);
        key.setReadOnly(!create);
        add(key);

        add(new Accordion("cond", Collections.<ITab>singletonList(
                new AbstractTab(new ResourceModel("cond", "Dynamic Condition")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public Panel getPanel(final String panelId) {
                return new UserSearchPanel.Builder(
                        new PropertyModel<List<SearchClause>>(dynRealmWrapper, "dynClauses")).
                        required(false).build(panelId);
            }
        }), Model.of(StringUtils.isBlank(dynRealmWrapper.getCond()) ? -1 : 0)).setOutputMarkupId(true));
    }

    @Override
    public DynRealmWrapper getItem() {
        return dynRealmWrapper;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            dynRealmWrapper.fillDynamicConditions();
            if (create) {
                restClient.create(dynRealmWrapper.getInnerObject());
            } else {
                restClient.update(dynRealmWrapper.getInnerObject());
            }
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating/updating dynamic realm", e);
            SyncopeConsoleSession.get().error(
                    StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);

    }

}
