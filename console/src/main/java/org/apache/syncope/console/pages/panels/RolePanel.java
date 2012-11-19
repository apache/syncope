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

import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.rest.EntitlementRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RolePanel extends Panel {

    private static final long serialVersionUID = 4216376097320768369L;

    @SpringBean
    private EntitlementRestClient entitlementRestClient;

    private final Palette<String> entitlementsPalette;

    public RolePanel(final String id, final Form form, final RoleTO roleTO) {
        super(id);

        this.add(new RoleDetailsPanel("details", roleTO, form).setOutputMarkupId(true));

        //--------------------------------
        // Attributes panel
        this.add(new AttributesPanel("attributes", roleTO, form, false).setOutputMarkupId(true));

        final AjaxCheckBoxPanel inhAttributes = new AjaxCheckBoxPanel("inheritAttributes", "inheritAttributes",
                new PropertyModel<Boolean>(roleTO, "inheritAttributes"));
        inhAttributes.setOutputMarkupId(true);
        this.add(inhAttributes);
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        this.add(new DerivedAttributesPanel("derivedAttributes", roleTO).setOutputMarkupId(true));

        final AjaxCheckBoxPanel inhDerivedAttributes = new AjaxCheckBoxPanel("inheritDerivedAttributes",
                "inheritDerivedAttributes", new PropertyModel<Boolean>(roleTO, "inheritDerivedAttributes"));
        inhDerivedAttributes.setOutputMarkupId(true);
        inhDerivedAttributes.setOutputMarkupId(true);
        this.add(inhDerivedAttributes);
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        this.add(new VirtualAttributesPanel("virtualAttributes", roleTO, false));

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

        ListModel<String> availableEntitlements = new ListModel<String>(entitlementRestClient.getAllEntitlements());

        entitlementsPalette = new Palette<String>("entitlementsPalette", selectedEntitlements, availableEntitlements,
                new SelectChoiceRenderer(), 20, false);

        this.add(entitlementsPalette);
    }

    public Palette<String> getEntitlementsPalette() {
        return this.entitlementsPalette;
    }
}
