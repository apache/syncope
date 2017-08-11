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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toggle panel.
 *
 * @param <T> model object type
 */
public abstract class TogglePanel<T extends Serializable> extends WizardMgtPanel<T> {

    private static final long serialVersionUID = -2025535531121434056L;

    protected static final Logger LOG = LoggerFactory.getLogger(TogglePanel.class);

    private enum Status {
        INACTIVE,
        ACTIVE

    }

    private final WebMarkupContainer container;

    private Status status = Status.INACTIVE;

    private final Label header;

    private final String activeId;

    public TogglePanel(final String id, final PageReference pageRef) {
        this(id, id, pageRef);
    }

    public TogglePanel(final String id, final String markupId, final PageReference pageRef) {
        super(id, true);
        this.activeId = markupId;
        setRenderBodyOnly(true);
        setOutputMarkupId(true);
        disableContainerAutoRefresh();
        setPageRef(pageRef);

        container = new WebMarkupContainer("togglePanelContainer");
        super.addInnerObject(container.setMarkupId(markupId == null ? id : markupId));

        header = new Label("label", StringUtils.EMPTY);
        header.setOutputMarkupId(true);
        container.add(header);

        container.add(new AjaxLink<Void>("close") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                toggle(target, false);
            }

        }.add(new AjaxEventBehavior(Constants.ON_CLICK) {

            private static final long serialVersionUID = -9027652037484739586L;

            @Override
            protected String findIndicatorId() {
                return StringUtils.EMPTY;
            }

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                // do nothing
            }
        }));
    }

    /**
     * Add object inside the main container.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    @Override
    public TogglePanel<T> addInnerObject(final Component... childs) {
        container.addOrReplace(childs);
        return this;
    }

    protected void setHeader(final AjaxRequestTarget target, final String header) {
        this.header.setDefaultModelObject(header == null
                ? StringUtils.EMPTY
                : header.length() >= 40 ? (header.substring(0, 30) + " ... ") : header);
        target.add(this.header);
    }

    public void close(final AjaxRequestTarget target) {
        toggle(target, false);
    }

    /**
     * Force toggle via java. To be used when the onclick has been intercepted before.
     *
     * @param target ajax request target.
     * @param toggle toggle action.
     */
    public void toggle(final AjaxRequestTarget target, final boolean toggle) {
        final String selector = String.format("$(\"div#%s\")", activeId);
        if (toggle) {
            if (status == Status.INACTIVE) {
                target.add(TogglePanel.this.container);
                target.appendJavaScript(
                        selector + ".toggle(\"slow\");"
                        + selector + ".attr(\"class\", \"toggle-menu active-toggle-menu\");");
                status = Status.ACTIVE;
            } else if (status == Status.ACTIVE) {
                // useful when handling action menu after refreshing (ref. SYNCOPE-1134)
                target.appendJavaScript(
                        selector + ".not(':visible')" + ".toggle(\"slow\")" + ".removeClass(\"inactive-toggle-menu\")"
                        + ".addClass(\"active-toggle-menu\");");
            }
        } else if (status == Status.ACTIVE) {
            target.appendJavaScript(
                    selector + ".toggle(\"slow\");"
                    + selector + ".attr(\"class\", \"toggle-menu inactive-toggle-menu\");");
            status = Status.INACTIVE;
        }
    }
}
