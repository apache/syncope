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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
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
import org.apache.wicket.model.ResourceModel;

public class DerAttrs extends WizardStep {

    private static final long serialVersionUID = -5387344116983102292L;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    public <T extends AnyTO> DerAttrs(final T entityTO, final String... anyTypeClass) {

        setOutputMarkupId(true);

        final IModel<List<String>> derSchemas = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<DerSchemaTO> derSchemaNames = schemaRestClient.getSchemas(SchemaType.DERIVED, anyTypeClass);

                return new ArrayList<>(CollectionUtils.collect(derSchemaNames, new Transformer<DerSchemaTO, String>() {

                    @Override
                    public String transform(final DerSchemaTO input) {
                        return input.getKey();
                    }
                }));
            }
        };

        final Map<String, AttrTO> derAttrMap = entityTO.getDerAttrMap();
        CollectionUtils.collect(derSchemas.getObject(), new Transformer<String, AttrTO>() {

            @Override
            public AttrTO transform(final String input) {
                AttrTO attrTO = derAttrMap.get(input);
                if (attrTO == null) {
                    attrTO = new AttrTO();
                    attrTO.setSchema(input);
                }
                return attrTO;
            }
        }, entityTO.getDerAttrs());

        final Fragment fragment;
        if (entityTO.getDerAttrs().isEmpty()) {
            // show empty list message
            fragment = new Fragment("content", "empty", this);
        } else {
            fragment = new Fragment("content", "attributes", this);

            final WebMarkupContainer attributesContainer = new WebMarkupContainer("derAttrContainer");
            attributesContainer.setOutputMarkupId(true);
            fragment.add(attributesContainer);

            ListView<AttrTO> attributes = new ListView<AttrTO>("attrs",
                    new PropertyModel<List<AttrTO>>(entityTO, "derAttrs") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public List<AttrTO> getObject() {
                            return new ArrayList<>(entityTO.getDerAttrs());
                        }

                    }) {

                        private static final long serialVersionUID = 9101744072914090143L;

                        @Override
                        protected void populateItem(final ListItem<AttrTO> item) {
                            final AttrTO attrTO = item.getModelObject();

                            final IModel<String> model;
                            final List<String> values = attrTO.getValues();
                            if (values == null || values.isEmpty()) {
                                model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                            } else {
                                model = new Model<String>(values.get(0));
                            }

                            final AjaxTextFieldPanel panel = new AjaxTextFieldPanel("panel", attrTO.getSchema(), model);

                            panel.setEnabled(false);
                            panel.setRequired(true);
                            panel.setOutputMarkupId(true);
                            item.add(panel);

                        }
                    };
            attributesContainer.add(attributes);
        }

        add(fragment);
    }
}
