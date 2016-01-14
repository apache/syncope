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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.StatusModal;
import org.apache.syncope.client.console.pages.UserDisplayAttributesModalPage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class UserSearchResultPanel extends AnySearchResultPanel<UserTO> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected UserSearchResultPanel(final String id, final Builder builder) {
        super(id, builder);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_USERS_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<UserTO, String>> getColumns() {

        final List<IColumn<UserTO, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(UserTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new PropertyColumn<UserTO, String>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<UserTO, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn<UserTO, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW)) {
            if (schemaNames.contains(name)) {
                columns.add(new AttrColumn<UserTO>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<UserTO>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : UserDisplayAttributesModalPage.USER_DEFAULT_SELECTION) {
                columns.add(new PropertyColumn<UserTO, String>(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(), Constants.PREF_USERS_DETAILS_VIEW,
                    Arrays.asList(UserDisplayAttributesModalPage.USER_DEFAULT_SELECTION));
        }

        columns.add(new ActionColumn<UserTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<UserTO> getActions(final String componentId, final IModel<UserTO> model) {

                final ActionLinksPanel.Builder<UserTO> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        final IModel<AnyHandler<UserTO>> formModel =
                                new CompoundPropertyModel<>(new AnyHandler<>(model.getObject()));
                        modal.setFormModel(formModel);

                        target.add(modal.setContent(new StatusModal<>(
                                modal, page.getPageReference(), formModel.getObject().getInnerObject())));

                        modal.header(new Model<>(
                                getString("any.edit", new Model<>(new AnyHandler<>(model.getObject())))));

                        modal.show(true);
                    }
                }, ActionLink.ActionType.MANAGE_RESOURCES, StandardEntitlement.USER_READ).add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        final IModel<AnyHandler<UserTO>> formModel =
                                new CompoundPropertyModel<>(new AnyHandler<>(model.getObject()));
                        modal.setFormModel(formModel);

                        target.add(modal.setContent(new StatusModal<>(
                                modal, page.getPageReference(), formModel.getObject().getInnerObject(), true)));

                        modal.header(new Model<>(
                                getString("any.edit", new Model<>(new AnyHandler<>(model.getObject())))));

                        modal.show(true);
                    }
                }, ActionLink.ActionType.ENABLE, StandardEntitlement.USER_READ).add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        send(UserSearchResultPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(
                                        new AnyHandler<>(new UserRestClient().read(model.getObject().getKey())),
                                        target));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.USER_READ).add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        final UserTO clone = SerializationUtils.clone(model.getObject());
                        clone.setKey(0L);
                        send(UserSearchResultPanel.this, Broadcast.EXACT,
                                new AjaxWizard.NewItemActionEvent<>(new AnyHandler<>(clone), target));
                    }
                }, ActionLink.ActionType.CLONE, StandardEntitlement.USER_CREATE).add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        try {
                            restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                        }
                        ((BasePage) getPage()).getNotificationPanel().refresh(target);

                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.USER_DELETE);

                return panel.build(componentId, model.getObject());
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder(page.getPageReference());

                return panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        target.add(modal.setContent(new UserDisplayAttributesModalPage<>(
                                modal, page.getPageReference(), schemaNames, dSchemaNames)));

                        modal.header(new ResourceModel("any.attr.display", ""));
                        modal.show(true);
                    }
                }, ActionLink.ActionType.CHANGE_VIEW, StandardEntitlement.USER_READ).add(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.USER_SEARCH).build(componentId);
            }
        });

        return columns;
    }

    public static class Builder extends AnySearchResultPanel.Builder<UserTO> {

        private static final long serialVersionUID = 1L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final AbstractAnyRestClient<UserTO> restClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, restClient, type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyHandler<UserTO>> newInstance(final String id) {
            return new UserSearchResultPanel(id, this);
        }
    }
}
