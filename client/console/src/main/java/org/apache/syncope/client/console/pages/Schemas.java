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

import static org.apache.wicket.Component.ENABLE;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.SchemasPanel;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.syncope.client.console.panels.SchemaModalPanel;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

public class Schemas extends BasePage {

    private static final long serialVersionUID = 8091922398776299403L;

    private final BaseModal<AbstractSchemaTO> modal;

    public Schemas(final PageParameters parameters) {
        super(parameters);

        this.modal = new BaseModal<>("modal");

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.add(new Label("header", "AnyTypeClasses/Schemas"));
        content.setOutputMarkupId(true);
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList(getPageReference())));
        content.add(buildCreateSchemaLink());
        add(content);

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add();
                modal.show(false);

                if (((AbstractBasePage) Schemas.this.getPage()).isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    ((AbstractBasePage) Schemas.this.getPage()).setModalResult(false);
                }
            }
        });

        add(modal);

    }

    private List<ITab> buildTabList(final PageReference pageReference) {

        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("AnyTypeClasses")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
//                final AnyTypeClassesPanel objectClassesPanel =
//                        new AnyTypeClassesPanel(panelId, getPageReference(), false);
//                objectClassesPanel.setEnabled(false);
//                return objectClassesPanel;
                return new EmptyPanel(panelId);
            }
        });

        tabs.add(new AbstractTab(new Model<>("Schemas")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new SchemasPanel(panelId, getPageReference(), modal);
            }
        });

        return tabs;
    }

    private AjaxLink<Void> buildCreateSchemaLink() {

        final AjaxLink<Void> createLink = new ClearIndicatingAjaxLink<Void>("createSchemaLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                modal.header(new ResourceModel("createSchema"));

                final SchemaModalPanel panel = new SchemaModalPanel(modal, getPageReference(), true);

                target.add(modal.setContent(panel));
                modal.addSumbitButton();
                modal.show(true);
            }
        };

        if (SyncopeConsoleSession.get().owns(Entitlement.SCHEMA_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE, Entitlement.SCHEMA_CREATE);
        }

        return createLink;
    }
}
