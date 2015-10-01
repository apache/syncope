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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.StatusModalPage;
import org.apache.syncope.client.console.pages.UserDisplayAttributesModalPage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class UserSearchResultPanel extends AnySearchResultPanel<UserTO> {

    private static final long serialVersionUID = -1100228004207271270L;

    private final String entitlement = "USER_LIST";

    public UserSearchResultPanel(final String type, final String parentId,
            final boolean filtered, final String fiql, final PageReference callerRef,
            final AbstractAnyRestClient restClient, final List<AnyTypeClassTO> anyTypeClassTOs, final String realm) {

        super(type, parentId, filtered, fiql, callerRef, restClient, anyTypeClassTOs, realm);
    }

    @Override
    protected List<IColumn<AnyTO, String>> getColumns() {

        final List<IColumn<AnyTO, String>> columns = new ArrayList<IColumn<AnyTO, String>>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(UserTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(
                        new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW)) {
            if (schemaNames.contains(name)) {
                columns.add(new AttrColumn(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : UserDisplayAttributesModalPage.USER_DEFAULT_SELECTION) {
                columns.add(
                        new PropertyColumn<AnyTO, String>(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(), Constants.PREF_USERS_DETAILS_VIEW,
                    Arrays.asList(UserDisplayAttributesModalPage.USER_DEFAULT_SELECTION));
        }

        columns.add(new ActionColumn<AnyTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<AnyTO> getActions(final String componentId, final IModel<AnyTO> model) {

                final ActionLinksPanel.Builder<AnyTO> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        final UserTO modelObject = UserTO.class.cast(model.getObject());

                        final IModel<UserTO> model = new CompoundPropertyModel<>(modelObject);
                        modal.setFormModel(model);

                        target.add(modal.setContent(
                                new StatusModalPage<UserTO>(modal, page.getPageReference(), model.getObject())));

                        modal.header(new Model<String>(MessageFormat.format(getString("any.edit"), anyTO.getKey())));
                        modal.show(true);
                    }
                }, ActionLink.ActionType.MANAGE_RESOURCES, entitlement).add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        final UserTO modelObject = UserTO.class.cast(model.getObject());

                        final IModel<UserTO> model = new CompoundPropertyModel<>(modelObject);
                        modal.setFormModel(model);

                        target.add(modal.setContent(
                                new StatusModalPage<UserTO>(modal, page.getPageReference(), model.getObject(), true)));

                        modal.header(new Model<String>(MessageFormat.format(getString("any.edit"), anyTO.getKey())));
                        modal.show(true);
                    }
                }, ActionLink.ActionType.ENABLE, entitlement).add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        final UserTO modelObject = ((UserRestClient) restClient).read(model.getObject().getKey());

                        final IModel<UserTO> model = new CompoundPropertyModel<>(modelObject);
                        modal.setFormModel(model);

                        target.add(modal);

                        modal.header(new Model<String>(MessageFormat.format(getString("any.edit"), anyTO.getKey())));
                        modal.show(true);
                    }
                }, ActionLink.ActionType.EDIT, entitlement).add(new ActionLink<AnyTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyTO anyTO) {
                        try {
                            restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            LOG.error("While deleting object {}", anyTO.getKey(), e);
                        }
                        ((BasePage) getPage()).getFeedbackPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, entitlement);

                return panel.build(componentId, model.getObject());
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        target.add(modal.setContent(new UserDisplayAttributesModalPage(
                                modal, page.getPageReference(), schemaNames, dSchemaNames)));

                        modal.header(new ResourceModel("any.attr.display", ""));
                        modal.show(true);
                    }
                }, ActionLink.ActionType.CHANGE_VIEW, entitlement).add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, entitlement);

                return panel.build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected <T extends AnyTO> Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();

        bulkActions.add(ActionType.DELETE);
        bulkActions.add(ActionType.SUSPEND);
        bulkActions.add(ActionType.REACTIVATE);

        return bulkActions;
    }

    @Override
    protected String getPageId() {
        return pageID;
    }
}
