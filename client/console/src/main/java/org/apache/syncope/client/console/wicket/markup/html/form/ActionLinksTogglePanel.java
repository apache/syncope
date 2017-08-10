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
package org.apache.syncope.client.console.wicket.markup.html.form;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;

public class ActionLinksTogglePanel<T extends Serializable> extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final WebMarkupContainer container;

    public ActionLinksTogglePanel(final String id, final PageReference pageRef) {
        super(id, UUID.randomUUID().toString(), pageRef);

        modal.size(Modal.Size.Large);
        setFooterVisibility(false);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void toggleWithContent(
            final AjaxRequestTarget target, final ActionsPanel<T> actionsPanel, final T modelObject) {

        setHeader(target, StringUtils.abbreviate(getHeader(modelObject), 25));

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
            }
        });

        final Fragment frag = new Fragment("actions", "actionsFragment", this);
        frag.setOutputMarkupId(true);
        frag.add(actionsPanel);

        container.addOrReplace(frag);
        target.add(this.container);

        this.toggle(target, true);
    }

    private Fragment getEmptyFragment() {
        return new Fragment("actions", "emptyFragment", this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ActionLinkToggleCloseEventPayload) {
            close(ActionLinkToggleCloseEventPayload.class.cast(event.getPayload()).getTarget());
        } else if (event.getPayload() instanceof ActionLinkToggleUpdateEventPayload) {
            String header = getHeader((T) ActionLinkToggleUpdateEventPayload.class.cast(event.
                    getPayload()).getModelObj());
            if (StringUtils.isNotBlank(header)) {
                setHeader(ActionLinkToggleUpdateEventPayload.class.cast(event.getPayload()).getTarget(),
                        StringUtils.abbreviate(header, 25));
            }
        }
    }

    public static class ActionLinkToggleCloseEventPayload {

        private final AjaxRequestTarget target;

        public ActionLinkToggleCloseEventPayload(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
