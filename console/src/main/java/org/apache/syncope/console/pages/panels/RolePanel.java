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
package org.apache.syncope.console.pages.panels;

import static org.apache.wicket.Component.RENDER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.RoleModalPage;
import org.apache.syncope.console.rest.AuthRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RolePanel extends Panel {

    private static final long serialVersionUID = 4216376097320768369L;

    @SpringBean
    private AuthRestClient entitlementRestClient;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    private final AjaxPalettePanel entitlementsPalette;

    public RolePanel(final String id, final Form form, final RoleTO roleTO, final RoleModalPage.Mode mode) {
        this(id, form, roleTO, mode, null);
    }

    public RolePanel(final String id, final Form form, final RoleTO roleTO, final RoleModalPage.Mode mode,
            final PageReference pageref) {

        super(id);

        this.add(new RoleDetailsPanel("details", roleTO, form, mode == RoleModalPage.Mode.TEMPLATE));

        if (pageref == null || roleTO.getId() == 0) {
            this.add(new Label("statuspanel", ""));
        } else {
            StatusPanel statusPanel = new StatusPanel("statuspanel", roleTO, new ArrayList<StatusBean>(), pageref);
            statusPanel.setOutputMarkupId(true);
            MetaDataRoleAuthorizationStrategy.authorize(
                    statusPanel, RENDER, xmlRolesReader.getAllAllowedRoles("Resources", "getConnectorObject"));
            this.add(statusPanel);
        }

        //--------------------------------
        // Attributes panel
        this.add(new AttributesPanel("attributes", roleTO, form, mode == RoleModalPage.Mode.TEMPLATE));

        final AjaxCheckBoxPanel inhAttributes = new AjaxCheckBoxPanel("inheritAttributes", "inheritAttributes",
                new PropertyModel<Boolean>(roleTO, "inheritAttributes"));
        inhAttributes.setOutputMarkupId(true);
        this.add(inhAttributes);
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        this.add(new DerivedAttributesPanel("derivedAttributes", roleTO));

        final AjaxCheckBoxPanel inhDerivedAttributes = new AjaxCheckBoxPanel("inheritDerivedAttributes",
                "inheritDerivedAttributes", new PropertyModel<Boolean>(roleTO, "inheritDerivedAttributes"));
        inhDerivedAttributes.setOutputMarkupId(true);
        inhDerivedAttributes.setOutputMarkupId(true);
        this.add(inhDerivedAttributes);
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        this.add(new VirtualAttributesPanel("virtualAttributes", roleTO, mode == RoleModalPage.Mode.TEMPLATE));

        final AjaxCheckBoxPanel inhVirtualAttributes = new AjaxCheckBoxPanel("inheritVirtualAttributes",
                "inheritVirtualAttributes", new PropertyModel<Boolean>(roleTO, "inheritVirtualAttributes"));
        inhVirtualAttributes.setOutputMarkupId(true);
        inhVirtualAttributes.setOutputMarkupId(true);
        this.add(inhVirtualAttributes);
        //--------------------------------

        //--------------------------------
        // Security container
        //--------------------------------

        this.add(new RoleSecurityPanel("security", roleTO).setOutputMarkupId(true));
        //--------------------------------

        //--------------------------------
        // Resources container
        //--------------------------------

        this.add(new ResourcesPanel("resources", roleTO).setOutputMarkupId(true));
        //--------------------------------

        ListModel<String> selectedEntitlements = new ListModel<String>(roleTO.getEntitlements());

        List<String> allEntitlements = entitlementRestClient.getAllEntitlements();
        if (allEntitlements != null && !allEntitlements.isEmpty()) {
            Collections.sort(allEntitlements);
        }
        ListModel<String> availableEntitlements = new ListModel<String>(allEntitlements);

        entitlementsPalette = new AjaxPalettePanel("entitlementsPalette", selectedEntitlements, availableEntitlements);

        this.add(entitlementsPalette);
    }

    public Collection<String> getSelectedEntitlements() {
        return this.entitlementsPalette.getModelCollection();
    }
}
