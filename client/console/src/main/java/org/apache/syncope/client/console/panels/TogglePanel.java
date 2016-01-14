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
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Toggle panel.
 */
public abstract class TogglePanel extends Panel {

    private static final long serialVersionUID = -2025535531121434056L;

    private enum Status {
        INACTIVE,
        ACTIVE

    }

    protected final BaseModal<Serializable> modal;

    private Status status = Status.INACTIVE;

    private final Label header;

    public TogglePanel(final String id) {
        super(id);
        setRenderBodyOnly(true);
        setOutputMarkupId(true);

        this.modal = new BaseModal<Serializable>("resource-modal");
        add(modal);

        header = new Label("label", StringUtils.EMPTY);
        header.setOutputMarkupId(true);
        add(header);

        add(new AjaxLink<Void>("close") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                TogglePanel.this.toggle(target, false);
            }

        }.setMarkupId("close"));
    }

    protected void setHeader(final AjaxRequestTarget target, final String header) {
        this.header.setDefaultModelObject(header.length() >= 40 ? (header.substring(0, 30) + " ... ") : header);
        target.add(this.header);
    }

    /**
     * Force toggle via java. To be used when the onclick has been intercepted before.
     *
     * @param target ajax request target.
     * @param toggle toggle action.
     */
    public void toggle(final AjaxRequestTarget target, final boolean toggle) {
        if (toggle) {
            if (status == Status.INACTIVE) {
                target.appendJavaScript("$(\"div.inactive-topology-menu\").toggle(\"slow\");"
                        + "$(\"div.inactive-topology-menu\").attr(\"class\", \"topology-menu active-topology-menu\");");
                status = Status.ACTIVE;
            }
        } else if (status == Status.ACTIVE) {
            target.appendJavaScript("$(\"div.active-topology-menu\").toggle(\"slow\");"
                    + "$(\"div.active-topology-menu\").attr(\"class\", \"topology-menu inactive-topology-menu\");");
            status = Status.INACTIVE;
        }
    }
}
