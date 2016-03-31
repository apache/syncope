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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.status.StatusModal;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
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

public class AnyObjectSearchResultPanel extends AnySearchResultPanel<AnyObjectTO> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected AnyObjectSearchResultPanel(final String id, final Builder builder) {
        super(id, builder);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_ANYOBJECT_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AnyObjectTO, String>> getColumns() {
        final List<IColumn<AnyObjectTO, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(
                getRequest(), String.format(Constants.PREF_ANY_OBJECT_DETAILS_VIEW, type))) {

            final Field field = ReflectionUtils.findField(AnyObjectTO.class, name);

            if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<AnyObjectTO, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn<AnyObjectTO, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(
                getRequest(), String.format(Constants.PREF_ANY_OBJECT_PLAIN_ATTRS_VIEW, type))) {

            if (pSchemaNames.contains(name)) {
                columns.add(new AttrColumn<AnyObjectTO>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(
                getRequest(), String.format(Constants.PREF_ANY_OBJECT_DER_ATTRS_VIEW, type))) {

            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<AnyObjectTO>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : AnyObjectDisplayAttributesModalPanel.DEFAULT_SELECTION) {
                columns.add(new PropertyColumn<AnyObjectTO, String>(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(),
                    String.format(Constants.PREF_ANY_OBJECT_DETAILS_VIEW, type),
                    Arrays.asList(AnyObjectDisplayAttributesModalPanel.DEFAULT_SELECTION));
        }

        setWindowClosedReloadCallback(displayAttributeModal);

        columns.add(new ActionColumn<AnyObjectTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<AnyObjectTO> getActions(final String componentId, final IModel<AnyObjectTO> model) {
                final ActionLinksPanel.Builder<AnyObjectTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<AnyObjectTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                        final IModel<AnyHandler<AnyObjectTO>> formModel =
                                new CompoundPropertyModel<>(new AnyHandler<>(model.getObject()));
                        altDefaultModal.setFormModel(formModel);

                        target.add(altDefaultModal.setContent(new StatusModal<>(
                                altDefaultModal, pageRef, formModel.getObject().getInnerObject(), false)));

                        altDefaultModal.header(new Model<>(
                                getString("any.edit", new Model<>(new AnyHandler<>(model.getObject())))));

                        altDefaultModal.show(true);
                    }
                }, ActionLink.ActionType.MANAGE_RESOURCES, StandardEntitlement.USER_READ).
                        add(new ActionLink<AnyObjectTO>() {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                                send(AnyObjectSearchResultPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(
                                                new AnyHandler<>(new AnyObjectRestClient().read(model.getObject().
                                                        getKey())),
                                                target));
                            }
                        }, ActionLink.ActionType.EDIT, String.format("%s_%s", type, AnyEntitlement.READ)).
                        add(new ActionLink<AnyObjectTO>() {

                            private static final long serialVersionUID = -7978723352517770645L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                                final AnyObjectTO clone = SerializationUtils.clone(model.getObject());
                                clone.setKey(0L);
                                send(AnyObjectSearchResultPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.NewItemActionEvent<>(new AnyHandler<>(clone), target));
                            }
                        }, ActionLink.ActionType.CLONE, StandardEntitlement.USER_CREATE).
                        add(new ActionLink<AnyObjectTO>() {

                            private static final long serialVersionUID = -7978723352517770646L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                                try {
                                    restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                                    info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (SyncopeClientException e) {
                                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                                    error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                }
                                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, String.format("%s_%s", type, AnyEntitlement.DELETE));

                return panel.build(componentId, model.getObject());
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        target.add(displayAttributeModal.setContent(new AnyObjectDisplayAttributesModalPanel<>(
                                displayAttributeModal, page.getPageReference(), pSchemaNames, dSchemaNames, type)));
                        displayAttributeModal.addSumbitButton();
                        displayAttributeModal.header(new ResourceModel("any.attr.display"));
                        displayAttributeModal.show(true);
                    }
                }, ActionLink.ActionType.CHANGE_VIEW, String.format("%s_%s", type, AnyEntitlement.READ)).add(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, String.format("%s_%s", type, AnyEntitlement.SEARCH));

                return panel.build(componentId);
            }
        }
        );

        return columns;

    }

    public static class Builder extends AnySearchResultPanel.Builder<AnyObjectTO> {

        private static final long serialVersionUID = -6828423611982275641L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new AnyObjectRestClient(), type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyHandler<AnyObjectTO>> newInstance(final String id) {
            return new AnyObjectSearchResultPanel(id, this);
        }
    }
}
