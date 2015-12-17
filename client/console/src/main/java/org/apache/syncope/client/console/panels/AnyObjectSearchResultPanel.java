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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AnyDisplayAttributesModalPage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class AnyObjectSearchResultPanel<T extends AnyTO> extends AbstractSearchResultPanel<T> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected final SchemaRestClient schemaRestClient = new SchemaRestClient();

    protected final List<String> schemaNames;

    protected final List<String> dSchemaNames;

    protected final String pageID = "Any";

    protected final String entitlement;

    protected AnyObjectSearchResultPanel(
            final String id, final AbstractSearchResultPanel.Builder<T> builder, final String entitlement) {
        super(id, builder);

        modal.size(Modal.Size.Large);

        this.entitlement = entitlement;

        add(new Label("name", builder.type));

        this.schemaNames = new ArrayList<>();
        for (AnyTypeClassTO anyTypeClassTO : AnySearchResultPanelBuilder.class.cast(builder).getAnyTypeClassTOs()) {
            this.schemaNames.addAll(anyTypeClassTO.getPlainSchemas());
        }
        this.dSchemaNames = new ArrayList<>();
        for (AnyTypeClassTO anyTypeClassTO : AnySearchResultPanelBuilder.class.cast(builder).getAnyTypeClassTOs()) {
            this.dSchemaNames.addAll(anyTypeClassTO.getDerSchemas());
        }

        initResultTable();
    }

    @Override
    protected List<IColumn<T, String>> getColumns() {
        final List<IColumn<T, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(AnyObjectTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new PropertyColumn<T, String>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<T, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn<T, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_ATTRIBUTES_VIEW)) {
            if (schemaNames.contains(name)) {
                columns.add(new AttrColumn<T>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_ANY_DERIVED_ATTRIBUTES_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<T>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : AnyDisplayAttributesModalPage.ANY_DEFAULT_SELECTION) {
                columns.add(new PropertyColumn<T, String>(new ResourceModel(name, name), name, name));
            }

        }

        columns.add(new ActionColumn<T, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<T> getActions(final String componentId, final IModel<T> model) {
                final ActionLinksPanel.Builder<T> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<T>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final T ignore) {
                        send(AnyObjectSearchResultPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<AnyHandler<T>>(
                                        new AnyHandler<T>(
                                                new AnyObjectRestClient().<T>read(model.getObject().getKey())),
                                        target));
                    }
                }, ActionLink.ActionType.EDIT, entitlement).add(new ActionLink<T>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final T ignore) {
                        try {
                            restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
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
                        // still missing content
                        target.add(modal.setContent(new AnyDisplayAttributesModalPage<>(
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
        final List<ActionLink.ActionType> bulkActions = new ArrayList<>();

        bulkActions.add(ActionLink.ActionType.DELETE);
        bulkActions.add(ActionLink.ActionType.SUSPEND);
        bulkActions.add(ActionLink.ActionType.REACTIVATE);

        return bulkActions;
    }

    @Override
    protected String getPageId() {
        return pageID;
    }

    public interface AnySearchResultPanelBuilder extends Serializable {

        List<AnyTypeClassTO> getAnyTypeClassTOs();
    }

    public static final class Builder extends AbstractSearchResultPanel.Builder<AnyObjectTO>
            implements AnySearchResultPanelBuilder {

        private static final long serialVersionUID = -6828423611982275640L;

        private final List<AnyTypeClassTO> anyTypeClassTOs;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final AbstractAnyRestClient<AnyObjectTO> restClient,
                final String type,
                final PageReference pageRef) {

            super(restClient, type, pageRef);
            this.anyTypeClassTOs = anyTypeClassTOs;
        }

        @Override
        protected WizardMgtPanel<AnyHandler<AnyObjectTO>> newInstance(final String id) {
            return new AnyObjectSearchResultPanel<>(id, this, type + "_LIST");
        }

        @Override
        public List<AnyTypeClassTO> getAnyTypeClassTOs() {
            return this.anyTypeClassTOs;
        }
    }
}
