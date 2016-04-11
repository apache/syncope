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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class DerAttrs extends AbstractAttrs {

    private static final long serialVersionUID = -5387344116983102292L;

    public <T extends AnyTO> DerAttrs(final T entityTO, final String... anyTypeClass) {
        super(entityTO);
        setOutputMarkupId(true);

        final LoadableDetachableModel<List<AttrTO>> derAttrTOs = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AttrTO> load() {
                List<String> anyTypeClasses = CollectionUtils.collect(anyTypeClassRestClient.list(getAllAuxClasses()),
                        EntityTOUtils.<String, AnyTypeClassTO>keyTransformer(),
                        new ArrayList<>(Arrays.asList(anyTypeClass)));

                List<DerSchemaTO> derSchemas = Collections.emptyList();
                if (!anyTypeClasses.isEmpty()) {
                    derSchemas =
                            schemaRestClient.getSchemas(SchemaType.DERIVED, anyTypeClasses.toArray(new String[] {}));
                }

                final Map<String, AttrTO> currents = entityTO.getDerAttrMap();
                entityTO.getDerAttrs().clear();

                // This conversion from set to lis is required by the ListView.
                // Not performed by using collect parameter because entityTO change is required.
                return new ArrayList<>(
                        CollectionUtils.collect(derSchemas, new Transformer<DerSchemaTO, AttrTO>() {

                            @Override
                            public AttrTO transform(final DerSchemaTO input) {
                                AttrTO attrTO = currents.get(input.getKey());
                                if (attrTO == null) {
                                    attrTO = new AttrTO();
                                    attrTO.setSchema(input.getKey());
                                }
                                return attrTO;
                            }
                        }, entityTO.getDerAttrs()));
            }
        };

        final WebMarkupContainer attributesContainer = new WebMarkupContainer("derAttrContainer");
        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        ListView<AttrTO> attributes = new ListView<AttrTO>("attrs", derAttrTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (derAttrTOs.getObject().isEmpty()) {
                    response.render(OnDomReadyHeaderItem.forScript(
                            String.format("$('#emptyPlaceholder').append(\"%s\")", getString("attribute.empty.list"))));
                }
            }

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                super.onComponentTagBody(markupStream, openTag);
                openTag.put("class", "empty");
            }

            @Override
            protected void populateItem(final ListItem<AttrTO> item) {
                final AttrTO attrTO = item.getModelObject();

                final IModel<String> model;
                final List<String> values = attrTO.getValues();
                if (values == null || values.isEmpty()) {
                    model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                } else {
                    model = new Model<>(values.get(0));
                }

                final AjaxTextFieldPanel panel = new AjaxTextFieldPanel(
                        "panel", attrTO.getSchema(), model, false);

                panel.setEnabled(false);
                panel.setRequired(true);
                panel.setOutputMarkupId(true);
                item.add(panel);

            }
        };
        attributesContainer.add(attributes);
    }
}
