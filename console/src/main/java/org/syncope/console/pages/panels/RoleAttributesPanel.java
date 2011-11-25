/*
 * Copyright 2011 marco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages.panels;

import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.RoleTO;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.rest.EntitlementRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

public class RoleAttributesPanel extends Panel {

    private static final long serialVersionUID = 4216376097320768369L;

    @SpringBean
    private EntitlementRestClient entitlementRestClient;

    final Palette<String> entitlementsPalette;

    public RoleAttributesPanel(final String id,
            final Form form, final RoleTO roleTO) {

        super(id);

        //--------------------------------
        // Attributes panel
        //--------------------------------
        final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                "name", "name",
                new PropertyModel<String>(roleTO, "name"), false);
        name.addRequiredLabel();
        this.add(name);

        this.add(new AttributesPanel("attributes", roleTO, form, false));

        final AjaxCheckBoxPanel inhAttributes = new AjaxCheckBoxPanel(
                "inheritAttributes",
                "inheritAttributes",
                new PropertyModel<Boolean>(roleTO, "inheritAttributes"),
                false);
        this.add(inhAttributes);
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        this.add(new DerivedAttributesPanel("derivedAttributes", roleTO));

        final AjaxCheckBoxPanel inhDerivedAttributes = new AjaxCheckBoxPanel(
                "inheritDerivedAttributes",
                "inheritDerivedAttributes",
                new PropertyModel<Boolean>(roleTO, "inheritDerivedAttributes"),
                false);
        inhDerivedAttributes.setOutputMarkupId(true);
        this.add(inhDerivedAttributes);
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        this.add(new VirtualAttributesPanel("virtualAttributes", roleTO,
                false));

        final AjaxCheckBoxPanel inhVirtualAttributes = new AjaxCheckBoxPanel(
                "inheritVirtualAttributes",
                "inheritVirtualAttributes",
                new PropertyModel<Boolean>(roleTO, "inheritVirtualAttributes"),
                false);
        inhVirtualAttributes.setOutputMarkupId(true);
        this.add(inhVirtualAttributes);
        //--------------------------------

        //--------------------------------
        // Security container
        //--------------------------------

        this.add(new RoleSecurityPanel("security", roleTO));
        //--------------------------------

        //--------------------------------
        // Resources container
        //--------------------------------

        this.add(new ResourcesPanel("resources", roleTO));
        //--------------------------------

        ListModel<String> selectedEntitlements =
                new ListModel<String>(roleTO.getEntitlements());

        ListModel<String> availableEntitlements =
                new ListModel<String>(
                entitlementRestClient.getAllEntitlements());

        entitlementsPalette = new Palette(
                "entitlementsPalette", selectedEntitlements,
                availableEntitlements, new SelectChoiceRenderer(), 20, false);

        this.add(entitlementsPalette);
    }

    public Palette<String> getEntitlementsPalette() {
        return this.entitlementsPalette;
    }
}