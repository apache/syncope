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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.MapOfListModel;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.DynRealmWrapper;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DynRealmModalPanel extends AbstractModalPanel<DynRealmWrapper> {

    private static final long serialVersionUID = -3773196441177699452L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected DynRealmRestClient dynRealmRestClient;

    protected final DynRealmWrapper dynRealmWrapper;

    protected final boolean create;

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
                Constants.KEY_FIELD_NAME,
                Constants.KEY_FIELD_NAME,
                new PropertyModel<>(dynRealmWrapper.getInnerObject(), Constants.KEY_FIELD_NAME), false);
        key.setReadOnly(!create);
        key.setRequired(true);
        add(key);

        final LoadableDetachableModel<List<AnyTypeTO>> types = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AnyTypeTO> load() {
                return anyTypeRestClient.listAnyTypes();
            }
        };

        add(new ListView<>("dynMembershipCond", types) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<AnyTypeTO> item) {
                final String key = item.getModelObject().getKey();
                item.add(new Accordion("dynMembershipCond", List.of(
                        new AbstractTab(Model.of(key + " Dynamic Condition")) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public Panel getPanel(final String panelId) {
                        switch (item.getModelObject().getKind()) {
                            case USER:
                                return new UserSearchPanel.Builder(
                                        new MapOfListModel<>(dynRealmWrapper, "dynClauses", key), pageRef).
                                        required(false).build(panelId);

                            case GROUP:
                                return new GroupSearchPanel.Builder(
                                        new MapOfListModel<>(dynRealmWrapper, "dynClauses", key), pageRef).
                                        required(false).build(panelId);

                            case ANY_OBJECT:
                            default:
                                return new AnyObjectSearchPanel.Builder(
                                        key,
                                        new MapOfListModel<>(dynRealmWrapper, "dynClauses", key), pageRef).
                                        required(false).build(panelId);
                        }
                    }
                }), Model.of(StringUtils.isBlank(dynRealmWrapper.getDynMembershipConds().get(key)) ? -1 : 0)).
                        setOutputMarkupId(true));
            }
        });
    }

    @Override
    public DynRealmWrapper getItem() {
        return dynRealmWrapper;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            dynRealmWrapper.fillDynamicConditions();
            if (create) {
                dynRealmRestClient.create(dynRealmWrapper.getInnerObject());
            } else {
                dynRealmRestClient.update(dynRealmWrapper.getInnerObject());
            }
            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating/updating dynamic realm", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
