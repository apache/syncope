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
import java.util.List;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal.ModalEvent;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

/**
 * Modal window with Resource form.
 *
 * @param <T>
 */
public abstract class AbstractResourceModal<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 1734415311027284221L;

    protected final List<ITab> tabs;

    public AbstractResourceModal(final BaseModal<T> modal, final PageReference pageRef) {
        super(modal, pageRef);

        this.tabs = new ArrayList<>();
        add(new AjaxBootstrapTabbedPanel<ITab>("tabbedPanel", tabs));
    }

    private class AjaxBootstrapTabbedPanel<T extends ITab>
            extends de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel<T> {

        private static final long serialVersionUID = 1L;

        AjaxBootstrapTabbedPanel(final String id, final List<T> tabs) {
            super(id, tabs);
        }

        @Override
        protected WebMarkupContainer newLink(final String linkId, final int index) {
            return new AjaxSubmitLink(linkId) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                    setSelectedTab(index);
                    if (target != null) {
                        target.add(AjaxBootstrapTabbedPanel.this);
                    }
                    onAjaxUpdate(target);
                }

                @Override
                protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                    modal.getNotificationPanel().refresh(target);
                }
            };
        }

    }

    public static class CreateEvent extends ModalEvent {

        private final Serializable key;

        private final String displayName;

        private final Serializable parent;

        private final TopologyNode.Kind kind;

        public CreateEvent(
                final Serializable key,
                final String displayName,
                final TopologyNode.Kind kind,
                final Serializable parent,
                final AjaxRequestTarget target) {
            super(target);
            this.key = key;
            this.displayName = displayName;
            this.kind = kind;
            this.parent = parent;
        }

        public Serializable getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TopologyNode.Kind getKind() {
            return kind;
        }

        public Serializable getParent() {
            return parent;
        }
    }
}
