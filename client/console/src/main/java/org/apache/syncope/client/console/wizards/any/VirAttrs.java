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
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class VirAttrs extends AbstractAttrs<VirSchemaTO> {

    private static final long serialVersionUID = -7982691107029848579L;

    public <T extends AnyTO> VirAttrs(
            final T anyTO,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichVirAttrs) {

        super(anyTO, anyTypeClasses, whichVirAttrs);

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
            @SuppressWarnings("unchecked")
            protected void populateItem(final ListItem<AttrTO> item) {
                AttrTO attrTO = item.getModelObject();

                attrTO.setReadonly(attrTO.isReadonly());

                final AjaxTextFieldPanel panel
                        = new AjaxTextFieldPanel("panel", attrTO.getSchema(), new Model<String>(), false);

                if (mode == AjaxWizard.Mode.TEMPLATE) {
                    item.add(panel.enableJextHelp().setEnabled(!attrTO.isReadonly()));
                } else {
                    item.add(new MultiFieldPanel.Builder<>(
                            new PropertyModel<List<String>>(attrTO, "values")).build(
                            "panel",
                            attrTO.getSchema(),
                            panel).setEnabled(!attrTO.isReadonly()));
                }
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.VIRTUAL;
    }

    @Override
    protected Set<AttrTO> getAttrsFromAnyTO() {
        return anyTO.getVirAttrs();
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = anyTO.getVirAttrMap();

        for (VirSchemaTO schema : schemas.values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }

            attrs.add(attrTO);
        }

        anyTO.getVirAttrs().clear();
        anyTO.getVirAttrs().addAll(attrs);
    }
}
