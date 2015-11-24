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
package org.apache.syncope.client.console.pages;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AnyTypeClassModalPanel;
import org.apache.syncope.client.console.panels.AnyTypeClassesPanel;
import org.apache.syncope.client.console.panels.AnyTypeModalPanel;
import org.apache.syncope.client.console.panels.AnyTypePanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.SchemasPanel;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.syncope.client.console.panels.SchemaModalPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public class Types extends BasePage {

    private static final long serialVersionUID = 8091922398776299403L;

    private final BaseModal<AbstractSchemaTO> schemaModal;

    private final BaseModal<AnyTypeClassTO> anyTypeClassModal;

    private final BaseModal<AnyTypeTO> anyTypeModal;

    private final AjaxBootstrapTabbedPanel<ITab> tabbedPanel;

    private enum Type {
        SCHEMA,
        ANYTYPECLASS,
        ANYTYPE,
        RELATIONSHIPTYPE;

    }

    public Types(final PageParameters parameters) {
        super(parameters);

        this.schemaModal = new BaseModal<>("schemaModal");
        this.anyTypeClassModal = new BaseModal<>("anyTypeClassModal");
        this.anyTypeModal = new BaseModal<>("anyTypeModal");

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.add(new Label("header", "Types"));
        content.setOutputMarkupId(true);
        tabbedPanel = new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList());
        content.add(tabbedPanel);

        final AjaxLink<Void> createSchemaLink =
                buildCreateLink("createSchema", schemaModal, Type.SCHEMA);
        content.add(createSchemaLink);

        if (SyncopeConsoleSession.get().owns(StandardEntitlement.SCHEMA_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(createSchemaLink, ENABLE, StandardEntitlement.SCHEMA_CREATE);
        }

        final AjaxLink<Void> createAnyTypeClassLink =
                buildCreateLink("createAnyTypeClass", anyTypeClassModal, Type.ANYTYPECLASS);
        content.add(createAnyTypeClassLink);

        if (SyncopeConsoleSession.get().owns(StandardEntitlement.ANYTYPECLASS_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(
                    createAnyTypeClassLink, ENABLE, StandardEntitlement.ANYTYPECLASS_CREATE);
        }

        final AjaxLink<Void> createAnyTypeLink =
                buildCreateLink("createAnyType", anyTypeModal, Type.ANYTYPE);
        content.add(createAnyTypeLink);

        if (SyncopeConsoleSession.get().owns(StandardEntitlement.ANYTYPE_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(
                    createAnyTypeLink, ENABLE, StandardEntitlement.ANYTYPE_CREATE);
        }

        add(content);
        addWindowWindowClosedCallback(schemaModal);
        addWindowWindowClosedCallback(anyTypeClassModal);
        addWindowWindowClosedCallback(anyTypeModal);
        add(schemaModal);
        add(anyTypeClassModal);
        add(anyTypeModal);
    }

    private List<ITab> buildTabList() {

        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("AnyTypes")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new AnyTypePanel(panelId, getPageReference(), anyTypeModal);
            }
        });

        tabs.add(new AbstractTab(new Model<>("AnyTypeClasses")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new AnyTypeClassesPanel(panelId, getPageReference(), anyTypeClassModal);
            }
        });

        tabs.add(new AbstractTab(new Model<>("Schemas")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SchemasPanel(panelId, getPageReference(), schemaModal);
            }
        });

        return tabs;
    }

    private AjaxLink<Void> buildCreateLink(final String label, final BaseModal<?> modal, final Type type) {

        final AjaxLink<Void> createLink = new ClearIndicatingAjaxLink<Void>(label, getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                modal.header(new ResourceModel(label));
                target.add(modal.setContent(buildModalPanel(type)));
                modal.addSumbitButton();
                modal.show(true);
            }
        };

        return createLink;
    }

    private ModalPanel buildModalPanel(final Type type) {
        final ModalPanel panel;
        switch (type) {
            case ANYTYPECLASS:
                anyTypeClassModal.setFormModel(new AnyTypeClassTO());
                anyTypeClassModal.size(Modal.Size.Large);
                panel = new AnyTypeClassModalPanel(anyTypeClassModal, getPageReference(), true);
                break;
            case ANYTYPE:
                anyTypeModal.setFormModel(new AnyTypeTO());
                anyTypeModal.size(Modal.Size.Large);
                panel = new AnyTypeModalPanel(anyTypeModal, getPageReference(), true);
                break;
            case RELATIONSHIPTYPE:
            case SCHEMA:
            default:
                schemaModal.setFormModel(new PlainSchemaTO());
                panel = new SchemaModalPanel(schemaModal, getPageReference(), true);
        }
        return panel;
    }

    private void addWindowWindowClosedCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                tabbedPanel.setSelectedTab(tabbedPanel.getSelectedTab());
                target.add(tabbedPanel);
                modal.show(false);

                if (((AbstractBasePage) Types.this.getPage()).isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    ((AbstractBasePage) Types.this.getPage()).setModalResult(false);
                }
            }
        });
    }
}
