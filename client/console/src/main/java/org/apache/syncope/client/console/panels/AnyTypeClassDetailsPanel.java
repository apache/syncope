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
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class AnyTypeClassDetailsPanel extends Panel {

    private static final long serialVersionUID = 3321861543207340469L;

    private final AnyTypeClassTO anyTypeClassTO;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final List<String> availablePlainSchemas = schemaRestClient.getPlainSchemaNames();

    private final List<String> availableDerSchemas = schemaRestClient.getDerSchemaNames();

    private final List<String> availableVirSchemas = schemaRestClient.getVirSchemaNames();

    private static final List<String> LAYOUT_PARAMETERS =
            Arrays.asList(new String[] { "admin.user.layout", "self.user.layout",
        "admin.group.layout", "self.group.layout", "admin.membership.layout", "self.membership.layout" });

    public AnyTypeClassDetailsPanel(final String id, final AnyTypeClassTO anyTypeClassTO) {
        super(id);

        this.anyTypeClassTO = anyTypeClassTO;
        buildAvailableSchemas(anyTypeClassTO.getKey());

        final Form<AnyTypeClassTO> antTypeClassForm = new Form<>("form");
        antTypeClassForm.setModel(new CompoundPropertyModel<>(anyTypeClassTO));
        antTypeClassForm.setOutputMarkupId(true);
        add(antTypeClassForm);

        final AjaxTextFieldPanel key = new AjaxTextFieldPanel("key", getString("key"), new PropertyModel<String>(
                this.anyTypeClassTO, "key"));
        key.addRequiredLabel();
        key.setEnabled(anyTypeClassTO.getKey() == null || this.anyTypeClassTO.getKey().isEmpty());
        antTypeClassForm.add(key);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        antTypeClassForm.add(container);

        AjaxPalettePanel<String> plainSchema = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("plainSchemas",
                        new PropertyModel<List<String>>(this.anyTypeClassTO, "plainSchemas"),
                        new ListModel<>(availablePlainSchemas));
        plainSchema.hideLabel();
        plainSchema.setOutputMarkupId(true);
        container.add(plainSchema);

        AjaxPalettePanel<String> derSchema = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("derSchemas",
                        new PropertyModel<List<String>>(this.anyTypeClassTO, "derSchemas"),
                        new ListModel<>(availableDerSchemas));
        derSchema.hideLabel();
        derSchema.setOutputMarkupId(true);
        container.add(derSchema);

        AjaxPalettePanel<String> virSchema = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("virSchemas",
                        new PropertyModel<List<String>>(this.anyTypeClassTO, "virSchemas"),
                        new ListModel<>(availableVirSchemas));
        virSchema.hideLabel();
        virSchema.setOutputMarkupId(true);
        container.add(virSchema);
    }

    private void buildAvailableSchemas(final String key) {

        List<String> configurationSchemas = new ArrayList<>();
        CollectionUtils.collect(new ConfRestClient().list(), new Transformer<AttrTO, String>() {

            @Override
            public String transform(final AttrTO attrTO) {
                return attrTO.getSchema();
            }
        }, configurationSchemas);

        for (AnyTypeClassTO item : new AnyTypeClassRestClient().list()) {
            if (key == null || !item.getKey().equals(key)) {
                availablePlainSchemas.removeAll(item.getPlainSchemas());
                availableDerSchemas.removeAll(item.getDerSchemas());
                availableVirSchemas.removeAll(item.getVirSchemas());
            }
        }

        availablePlainSchemas.removeAll(configurationSchemas);
        availablePlainSchemas.removeAll(LAYOUT_PARAMETERS);
    }
}
