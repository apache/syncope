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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.notifications.NotificationTasks;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.status.AnyStatusModal;
import org.apache.syncope.client.console.tasks.AnyPropagationTasks;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.springframework.util.ReflectionUtils;

public class AnyObjectDirectoryPanel extends AnyDirectoryPanel<AnyObjectTO, AnyObjectRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected AnyObjectDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);
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

            addPropertyColumn(name, ReflectionUtils.findField(AnyObjectTO.class, name), columns);
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
                addPropertyColumn(name, ReflectionUtils.findField(AnyObjectTO.class, name), columns);
            }

            prefMan.setList(getRequest(), getResponse(),
                    String.format(Constants.PREF_ANY_OBJECT_DETAILS_VIEW, type),
                    Arrays.asList(AnyObjectDisplayAttributesModalPanel.DEFAULT_SELECTION));
        }

        columns.add(new ActionColumn<AnyObjectTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<AnyObjectTO> getActions(final String componentId, final IModel<AnyObjectTO> model) {
                final ActionLinksPanel.Builder<AnyObjectTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<AnyObjectTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                        send(AnyObjectDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(
                                        new AnyWrapper<>(new AnyObjectRestClient().read(model.getObject().getKey())),
                                        target));
                    }
                }, ActionType.EDIT, AnyEntitlement.READ.getFor(type)).add(new ActionLink<AnyObjectTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                        final AnyObjectTO clone = SerializationUtils.clone(model.getObject());
                        clone.setKey(null);
                        send(AnyObjectDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.NewItemActionEvent<>(new AnyWrapper<>(clone), target));
                    }

                    @Override
                    protected boolean statusCondition(final AnyObjectTO modelObject) {
                        return addAjaxLink.isVisibleInHierarchy();
                    }
                }, ActionType.CLONE, AnyEntitlement.CREATE.getFor(type)).add(new ActionLink<AnyObjectTO>() {

                    private static final long serialVersionUID = -7978723352517770646L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                        try {
                            restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.DELETE, AnyEntitlement.DELETE.getFor(type));

                if (wizardInModal) {
                    panel.add(new ActionLink<AnyObjectTO>() {

                        private static final long serialVersionUID = -7978723352517770645L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                            final IModel<AnyWrapper<AnyObjectTO>> formModel = new CompoundPropertyModel<>(
                                    new AnyWrapper<>(model.getObject()));
                            altDefaultModal.setFormModel(formModel);

                            target.add(altDefaultModal.setContent(new AnyStatusModal<>(
                                    altDefaultModal,
                                    pageRef,
                                    formModel.getObject().getInnerObject(),
                                    "resourceName",
                                    false)));

                            altDefaultModal.header(new Model<>(
                                    getString("any.edit", new Model<>(new AnyWrapper<>(model.getObject())))));

                            altDefaultModal.show(true);
                        }
                    }, ActionType.MANAGE_RESOURCES, AnyEntitlement.READ.getFor(type)).add(
                            new ActionLink<AnyObjectTO>() {

                        private static final long serialVersionUID = -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                            target.add(utilityModal.setContent(new AnyPropagationTasks(
                                    utilityModal, AnyTypeKind.ANY_OBJECT, model.getObject().getKey(), pageRef)));

                            utilityModal.header(new StringResourceModel("any.propagation.tasks", model));
                            utilityModal.show(true);
                        }
                    }, ActionType.PROPAGATION_TASKS, StandardEntitlement.TASK_LIST).add(new ActionLink<AnyObjectTO>() {

                                private static final long serialVersionUID = -7978723352517770644L;

                                @Override
                                public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                                    target.add(utilityModal.setContent(
                                            new NotificationTasks(AnyTypeKind.ANY_OBJECT, model.getObject().getKey(),
                                                    pageRef)));
                                    utilityModal.header(new StringResourceModel("any.notification.tasks", model));
                                    utilityModal.show(true);
                                    target.add(utilityModal);
                                }
                            }, ActionType.NOTIFICATION_TASKS, StandardEntitlement.TASK_LIST);
                }

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
                        displayAttributeModal.addSubmitButton();
                        displayAttributeModal.header(new ResourceModel("any.attr.display"));
                        displayAttributeModal.show(true);
                    }

                    @Override
                    protected boolean statusCondition(final Serializable modelObject) {
                        return wizardInModal;
                    }
                }, ActionType.CHANGE_VIEW, AnyEntitlement.READ.getFor(type)).add(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionType.RELOAD, AnyEntitlement.SEARCH.getFor(type));

                return panel.build(componentId);
            }
        }
        );

        return columns;

    }

    public static class Builder extends AnyDirectoryPanel.Builder<AnyObjectTO, AnyObjectRestClient> {

        private static final long serialVersionUID = -6828423611982275641L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new AnyObjectRestClient(), type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<AnyObjectTO>> newInstance(final String id, final boolean wizardInModal) {
            return new AnyObjectDirectoryPanel(id, this, wizardInModal);
        }
    }
}
