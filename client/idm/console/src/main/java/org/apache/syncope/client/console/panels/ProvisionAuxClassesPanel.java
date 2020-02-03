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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProvisionAuxClassesPanel extends Panel {

    private static final long serialVersionUID = -3962956154520358784L;

    private static final Logger LOG = LoggerFactory.getLogger(ProvisionAuxClassesPanel.class);

    private final ProvisionTO provision;

    public ProvisionAuxClassesPanel(final String id, final ProvisionTO provision) {
        super(id);
        setOutputMarkupId(true);

        this.provision = provision;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        AnyTypeTO anyType = null;
        IModel<List<String>> model;
        List<String> choices;
        if (provision == null) {
            model = new ListModel<>(List.of());
            choices = List.of();
        } else {
            model = new PropertyModel<>(provision, "auxClasses");
            choices = new ArrayList<>();

            try {
                anyType = AnyTypeRestClient.read(provision.getAnyType());
            } catch (Exception e) {
                LOG.error("Could not read AnyType {}", provision.getAnyType(), e);
            }
            if (anyType != null) {
                for (AnyTypeClassTO aux : AnyTypeClassRestClient.list()) {
                    if (!anyType.getClasses().contains(aux.getKey())) {
                        choices.add(aux.getKey());
                    }
                }
            }
        }

        addOrReplace(
                new AjaxPalettePanel.Builder<String>().build("auxClasses", model, new ListModel<>(choices)).
                        hideLabel().
                        setOutputMarkupId(true).
                        setEnabled(provision != null));

        AjaxTextFieldPanel uidOnCreate = new AjaxTextFieldPanel(
                "uidOnCreate", new ResourceModel("uidOnCreate", "uidOnCreate").getObject(),
                new PropertyModel<>(provision, "uidOnCreate"));
        uidOnCreate.setChoices(getSchemas(anyType, model.getObject()));
        uidOnCreate.setOutputMarkupId(true).setEnabled(provision != null);
        addOrReplace(uidOnCreate);
    }

    private static List<String> getSchemas(final AnyTypeTO anyType, final List<String> anyTypeClasses) {
        List<String> classes = new ArrayList<>(anyType.getClasses());
        classes.addAll(anyTypeClasses);

        return SchemaRestClient.<PlainSchemaTO>getSchemas(
                SchemaType.PLAIN, null, classes.toArray(new String[] {})).
                stream().map(EntityTO::getKey).collect(Collectors.toList());
    }
}
