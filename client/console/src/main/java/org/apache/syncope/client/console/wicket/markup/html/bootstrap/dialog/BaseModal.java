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
package org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseModal<T> extends Modal<T> {

    private static final long serialVersionUID = -6142277554912316095L;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseModal.class);

    private static final String CONTENT = "modalContent";
    
    private static final String INPUT = "input";

    protected NotificationPanel feedbackPanel;

    private final List<Component> components;

    private final WebMarkupContainer container;

    private WindowClosedCallback windowClosedCallback;

    public BaseModal(final String id) {
        super(id);
        container = new WebMarkupContainer("container");
        container.add(new EmptyPanel(CONTENT));
        container.setOutputMarkupId(true);
        add(container);
        setUseCloseHandler(true);
        this.windowClosedCallback = null;
        components = new ArrayList<>();
    }

    @Override
    public MarkupContainer addOrReplace(final Component... component) {
        return container.addOrReplace(component);
    }

    public static String getModalContentId() {
        return CONTENT;
    }

    public static String getModalInputId() {
        return INPUT;
    }

    public BaseModal<T> setWindowClosedCallback(final WindowClosedCallback callback) {
        windowClosedCallback = callback;
        return this;
    }

    @Override
    protected void onClose(final IPartialPageRequestHandler target) {
        if (windowClosedCallback != null) {
            windowClosedCallback.onClose((AjaxRequestTarget) target);
        }
    }

    public void addFooterInput(final Component component) {
        this.components.add(component);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final WebMarkupContainer footer = (WebMarkupContainer) this.get("dialog:footer");
        footer.addOrReplace(new ListView<Component>("inputs", components) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }
        }.setOutputMarkupId(true));
    }
}
