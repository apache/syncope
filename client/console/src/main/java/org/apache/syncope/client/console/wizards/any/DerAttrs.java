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
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class DerAttrs extends AbstractAttrs<DerSchemaTO> {

    private static final long serialVersionUID = -5387344116983102292L;

    public <T extends AnyTO> DerAttrs(
            final T anyTO,
            final List<String> anyTypeClasses,
            final List<String> whichDerAttrs) {

        super(anyTO, anyTypeClasses, whichDerAttrs);
        setTitleModel(new ResourceModel("attributes.derived"));

        add(new ListView<AttrTO>("schemas", attrTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (attrTOs.getObject().isEmpty()) {
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
                AttrTO attrTO = item.getModelObject();

                IModel<String> model;
                List<String> values = attrTO.getValues();
                if (values == null || values.isEmpty()) {
                    model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                } else {
                    model = new Model<>(values.get(0));
                }

                AjaxTextFieldPanel panel = new AjaxTextFieldPanel("panel", attrTO.getSchema(), model, false);
                panel.setEnabled(false);
                panel.setRequired(true);
                panel.setOutputMarkupId(true);
                item.add(panel);
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.DERIVED;
    }

    @Override
    protected Set<AttrTO> getAttrsFromAnyTO() {
        return anyTO.getDerAttrs();
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = anyTO.getDerAttrMap();

        for (DerSchemaTO schema : schemas.values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }

            attrs.add(attrTO);
        }

        anyTO.getDerAttrs().clear();
        anyTO.getDerAttrs().addAll(attrs);
    }

}
