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
package org.apache.syncope.client.console.wizards.any;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class VirAttrs extends WizardStep {

    private static final long serialVersionUID = -7982691107029848579L;

    private SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final Map<String, VirSchemaTO> schemas = new TreeMap<String, VirSchemaTO>();

    public <T extends AnyTO> VirAttrs(final T entityTO, final String... anyTypeClass) {
        this.setOutputMarkupId(true);

        final IModel<List<String>> virSchemas = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<VirSchemaTO> schemaTOs = schemaRestClient.getSchemas(SchemaType.VIRTUAL, anyTypeClass);

                schemas.clear();

                for (VirSchemaTO schemaTO : schemaTOs) {
                    schemas.put(schemaTO.getKey(), schemaTO);
                }

                return new ArrayList<>(schemas.keySet());
            }
        };

        final Map<String, AttrTO> virAttrMap = entityTO.getVirAttrMap();
        CollectionUtils.collect(virSchemas.getObject(), new Transformer<String, AttrTO>() {

            @Override
            public AttrTO transform(final String input) {
                AttrTO attrTO = virAttrMap.get(input);
                if (attrTO == null) {
                    attrTO = new AttrTO();
                    attrTO.setSchema(input);
                    attrTO.getValues().add(StringUtils.EMPTY);
                } else if (attrTO.getValues().isEmpty()) {
                    attrTO.getValues().add("");
                }

                return attrTO;
            }
        }, entityTO.getVirAttrs());

        final Fragment fragment;
        if (entityTO.getVirAttrs().isEmpty()) {
            // show empty list message
            fragment = new Fragment("content", "empty", this);
        } else {
            fragment = new Fragment("content", "attributes", this);

            final WebMarkupContainer attributesContainer = new WebMarkupContainer("virAttrContainer");
            attributesContainer.setOutputMarkupId(true);
            fragment.add(attributesContainer);

            ListView<AttrTO> attributes = new ListView<AttrTO>("attrs",
                    new PropertyModel<List<AttrTO>>(entityTO, "virAttrs") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public List<AttrTO> getObject() {
                            return new ArrayList<>(entityTO.getVirAttrs());
                        }

                    }) {

                        private static final long serialVersionUID = 9101744072914090143L;

                        @Override
                        @SuppressWarnings("unchecked")
                        protected void populateItem(final ListItem<AttrTO> item) {
                            AttrTO attrTO = item.getModelObject();
                            final VirSchemaTO schema = schemas.get(attrTO.getSchema());

                            attrTO.setReadonly(schema.isReadonly());

                            final AjaxTextFieldPanel panel = new AjaxTextFieldPanel(
                                    "panel", attrTO.getSchema(), new Model<String>(), false);

                            item.add(new MultiFieldPanel.Builder<String>(
                                            new PropertyModel<List<String>>(attrTO, "values")).build(
                                            "panel",
                                            schema.getKey(),
                                            panel).setEnabled(!schema.isReadonly()));
                        }
                    };

            attributesContainer.add(attributes);
        }

        add(fragment);
    }
}
