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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.XMLRolesReader;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.AuthRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class GroupPanel extends Panel {

    private static final long serialVersionUID = 4216376097320768369L;

    @SpringBean
    private AuthRestClient authRestClient;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    private final AjaxPalettePanel<String> entitlements;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 8150440254654306070L;

        private String id;

        private Form form;

        private GroupTO groupTO;

        private Mode mode;

        private PageReference pageReference;

        public Builder(final String id) {
            this.id = id;
        }

        public Builder form(final Form form) {
            this.form = form;
            return this;
        }

        public Builder groupTO(final GroupTO groupTO) {
            this.groupTO = groupTO;
            return this;
        }

        public Builder groupModalPageMode(final Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder pageRef(final PageReference pageReference) {
            this.pageReference = pageReference;
            return this;
        }

        public GroupPanel build() {
            return new GroupPanel(this);
        }
    }

    private GroupPanel(final Builder builder) {
        super(builder.id);

        this.add(new GroupDetailsPanel("details", builder.groupTO, builder.mode == Mode.TEMPLATE));

        if (builder.pageReference == null || builder.groupTO.getKey() == 0) {
            this.add(new Label("statuspanel", ""));
        } else {
            StatusPanel statusPanel = new StatusPanel(
                    "statuspanel", builder.groupTO, new ArrayList<StatusBean>(), builder.pageReference);
            statusPanel.setOutputMarkupId(true);
            MetaDataRoleAuthorizationStrategy.authorize(
                    statusPanel, RENDER, xmlRolesReader.getEntitlement("Resources", "getConnectorObject"));
            this.add(statusPanel);
        }

        this.add(new AnnotatedBeanPanel("systeminformation", builder.groupTO));

        //--------------------------------
        // Attribute templates panel
        //--------------------------------
        AttrTemplatesPanel attrTemplates = new AttrTemplatesPanel("templates", builder.groupTO);
        this.add(attrTemplates);

        //--------------------------------
        // Attributes panel
        //--------------------------------
        this.add(new PlainAttrsPanel(
                "plainAttrs", builder.groupTO, builder.form, builder.mode, attrTemplates));

        final AjaxCheckBoxPanel inhAttributes = new AjaxCheckBoxPanel("inheritPlainAttrs", "inheritPlainAttrs",
                new PropertyModel<Boolean>(builder.groupTO, "inheritPlainAttrs"));
        inhAttributes.setOutputMarkupId(true);
        this.add(inhAttributes);
        //--------------------------------

        //--------------------------------
        // Derived attributes panel
        //--------------------------------
        this.add(new DerAttrsPanel("derAttrs", builder.groupTO, attrTemplates));

        final AjaxCheckBoxPanel inhDerivedAttributes = new AjaxCheckBoxPanel("inheritDerAttrs",
                "inheritDerAttrs", new PropertyModel<Boolean>(builder.groupTO, "inheritDerAttrs"));
        inhDerivedAttributes.setOutputMarkupId(true);
        this.add(inhDerivedAttributes);
        //--------------------------------

        //--------------------------------
        // Virtual attributes panel
        //--------------------------------
        this.add(new VirAttrsPanel(
                "virAttrs", builder.groupTO, builder.mode == Mode.TEMPLATE, attrTemplates));

        final AjaxCheckBoxPanel inhVirtualAttributes = new AjaxCheckBoxPanel("inheritVirAttrs",
                "inheritVirAttrs", new PropertyModel<Boolean>(builder.groupTO, "inheritVirAttrs"));
        inhVirtualAttributes.setOutputMarkupId(true);
        this.add(inhVirtualAttributes);
        //--------------------------------

        //--------------------------------
        // Resources panel
        //--------------------------------
        this.add(new ResourcesPanel.Builder("resources").attributableTO(builder.groupTO).build().
                setOutputMarkupId(true));
        //--------------------------------

        //--------------------------------
        // Entitlements
        //--------------------------------
        ListModel<String> selectedEntitlements = new ListModel<String>(builder.groupTO.getEntitlements());

        List<String> allEntitlements = authRestClient.getAllEntitlements();
        if (allEntitlements != null && !allEntitlements.isEmpty()) {
            Collections.sort(allEntitlements);
        }
        ListModel<String> availableEntitlements = new ListModel<String>(allEntitlements);

        entitlements = new AjaxPalettePanel<String>("entitlements", selectedEntitlements, availableEntitlements);
        this.add(entitlements);

        //--------------------------------
        // Security panel
        //--------------------------------
        this.add(new GroupSecurityPanel("security", builder.groupTO).setOutputMarkupId(true));
        //--------------------------------
    }

    public Collection<String> getSelectedEntitlements() {
        return this.entitlements.getModelCollection();
    }
}
