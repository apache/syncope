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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ConsoleLogPanel;
import org.apache.syncope.client.console.panels.CoreLogPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.LoggerTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Logs extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    private final BaseModal<LoggerTO> coreLogModal;

    private final BaseModal<LoggerTO> consoleLogModal;

    private final AjaxBootstrapTabbedPanel<ITab> tabbedPanel;

    public Logs(final PageParameters parameters) {
        super(parameters);

        coreLogModal = new BaseModal<>("coreLogModal");
        consoleLogModal = new BaseModal<>("consoleLogModal");

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.add(new Label("header", "Logs"));
        content.setOutputMarkupId(true);
        tabbedPanel = new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList());
        content.add(tabbedPanel);

        add(content);
        addWindowWindowClosedCallback(coreLogModal);
        addWindowWindowClosedCallback(consoleLogModal);
        add(coreLogModal);
        add(consoleLogModal);
    }

    private List<ITab> buildTabList() {

        final List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new Model<>("Core")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new CoreLogPanel(panelId, getPageReference(), coreLogModal);
            }
        });

        tabs.add(new AbstractTab(new Model<>("Console")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ConsoleLogPanel(panelId, getPageReference(), consoleLogModal);
            }
        });

        return tabs;
    }

    private void addWindowWindowClosedCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                tabbedPanel.setSelectedTab(tabbedPanel.getSelectedTab());
                target.add(tabbedPanel);
                modal.show(false);

                if (((AbstractBasePage) Logs.this.getPage()).isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    ((AbstractBasePage) Logs.this.getPage()).setModalResult(false);
                }
            }
        });
    }
}
