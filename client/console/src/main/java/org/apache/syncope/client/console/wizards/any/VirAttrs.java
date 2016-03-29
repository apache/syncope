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
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class VirAttrs extends AbstractAttrs {

    private static final long serialVersionUID = -7982691107029848579L;

    public <T extends AnyTO> VirAttrs(final T entityTO, final String... anyTypeClass) {
        super(entityTO);
        this.setOutputMarkupId(true);

        final LoadableDetachableModel<List<AttrTO>> virAttrTOs = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<AttrTO> load() {
                List<String> anyTypeClasses = CollectionUtils.collect(anyTypeClassRestClient.list(getAllAuxClasses()),
                        EntityTOUtils.<String, AnyTypeClassTO>keyTransformer(),
                        new ArrayList<>(Arrays.asList(anyTypeClass)));

                List<VirSchemaTO> virSchemas = Collections.emptyList();
                if (!anyTypeClasses.isEmpty()) {
                    virSchemas =
                            schemaRestClient.getSchemas(SchemaType.VIRTUAL, anyTypeClasses.toArray(new String[] {}));
                }

                final Map<String, AttrTO> currents = entityTO.getVirAttrMap();
                entityTO.getVirAttrs().clear();

                // This conversion from set to lis is required by the ListView.
                // Not performed by using collect parameter because entityTO change is required.
                return new ArrayList<>(CollectionUtils.collect(virSchemas, new Transformer<VirSchemaTO, AttrTO>() {

                    @Override
                    public AttrTO transform(final VirSchemaTO input) {
                        AttrTO attrTO = currents.get(input.getKey());
                        if (attrTO == null) {
                            attrTO = new AttrTO();
                            attrTO.setSchema(input.getKey());
                            attrTO.getValues().add(StringUtils.EMPTY);
                        } else if (attrTO.getValues().isEmpty()) {
                            attrTO.getValues().add("");
                        }

                        attrTO.setReadonly(input.isReadonly());
                        return attrTO;
                    }
                }, entityTO.getVirAttrs()));
            }
        };

        final WebMarkupContainer attributesContainer = new WebMarkupContainer("virAttrContainer");
        attributesContainer.setOutputMarkupId(true);
        add(attributesContainer);

        ListView<AttrTO> attributes = new ListView<AttrTO>("attrs", virAttrTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (virAttrTOs.getObject().isEmpty()) {
                    response.render(OnDomReadyHeaderItem.forScript(
                            String.format("$('#emptyPlaceholder').append(\"%s\")", getString("attribute.empty.list"))));
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void populateItem(final ListItem<AttrTO> item) {
                AttrTO attrTO = item.getModelObject();

                attrTO.setReadonly(attrTO.isReadonly());

                final AjaxTextFieldPanel panel = new AjaxTextFieldPanel(
                        "panel", attrTO.getSchema(), new Model<String>(), false);

                item.add(new MultiFieldPanel.Builder<>(
                        new PropertyModel<List<String>>(attrTO, "values")).build(
                        "panel",
                        attrTO.getSchema(),
                        panel).setEnabled(!attrTO.isReadonly()));
            }
        };

        attributesContainer.add(attributes);
    }
}
