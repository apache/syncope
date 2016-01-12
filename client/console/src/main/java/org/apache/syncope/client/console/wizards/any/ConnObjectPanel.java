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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class ConnObjectPanel extends Panel {

    private static final long serialVersionUID = -6469290753080058487L;

    public ConnObjectPanel(final String id, final Pair<ConnObjectTO, ConnObjectTO> connObjectTOs) {
        super(id);

        final IModel<List<AttrTO>> formProps = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AttrTO> load() {
                List<AttrTO> attrs = new ArrayList<>(connObjectTOs == null || connObjectTOs.getRight() == null
                        ? Collections.<AttrTO>emptyList()
                        : connObjectTOs.getRight().getPlainAttrs());

                Collections.sort(attrs, new Comparator<AttrTO>() {

                    @Override
                    public int compare(final AttrTO attr1, final AttrTO attr2) {
                        if (attr1 == null || attr1.getSchema() == null) {
                            return -1;
                        }
                        if (attr2 == null || attr2.getSchema() == null) {
                            return 1;
                        }
                        return attr1.getSchema().compareTo(attr2.getSchema());
                    }
                });

                return attrs;
            }
        };

        final Map<String, AttrTO> beforeProfile = connObjectTOs.getLeft() == null
                ? null
                : connObjectTOs.getLeft().getPlainAttrMap();

        final ListView<AttrTO> propView = new ListView<AttrTO>("propView", formProps) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void populateItem(final ListItem<AttrTO> item) {
                final AttrTO prop = item.getModelObject();

                final Fragment valueFragment;
                if (beforeProfile == null) {
                    valueFragment = new Fragment("value", "singleValue", ConnObjectPanel.this);
                    valueFragment.add(getValuePanel("attribute", prop.getSchema(), prop));
                } else {
                    final AttrTO before = beforeProfile.get(prop.getSchema());

                    valueFragment = new Fragment("value", "doubleValue", ConnObjectPanel.this);
                    valueFragment.add(getValuePanel("oldAttribute", prop.getSchema(), before));
                    valueFragment.add(getValuePanel("newAttribute", prop.getSchema(), prop));

                    if (before == null
                            || (CollectionUtils.isNotEmpty(prop.getValues())
                            && CollectionUtils.isEmpty(before.getValues()))
                            || (CollectionUtils.isEmpty(prop.getValues())
                            && CollectionUtils.isNotEmpty(before.getValues()))
                            || (CollectionUtils.isNotEmpty(prop.getValues())
                            && CollectionUtils.isNotEmpty(before.getValues())
                            && prop.getValues().size() != before.getValues().size())
                            || (CollectionUtils.isNotEmpty(prop.getValues())
                            && CollectionUtils.isNotEmpty(before.getValues())
                            && !prop.getValues().equals(before.getValues()))) {
                        valueFragment.add(new Behavior() {

                            private static final long serialVersionUID = 3109256773218160485L;

                            @Override
                            public void onComponentTag(final Component component, final ComponentTag tag) {
                                tag.put("class", "highlight");
                            }
                        });
                    }
                }
                item.add(valueFragment);
            }
        };
        add(propView);
    }

    /**
     * Get panel for attribute value (not remote status).
     *
     * @param id component id to be replaced with the fragment content.
     * @param attrTO remote attribute.
     * @return fragment.
     */
    private Panel getValuePanel(final String id, final String schemaName, final AttrTO attrTO) {
        Panel field;
        if (attrTO == null) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<String>());
        } else if (CollectionUtils.isEmpty(attrTO.getValues())) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<String>());
        } else if (ConnIdSpecialAttributeName.PASSWORD.equals(schemaName)) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<String>("********"));
        } else if (attrTO.getValues().size() == 1) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<String>(attrTO.getValues().get(0)));
        } else {
            field = new MultiFieldPanel.Builder<String>(new ListModel<String>(attrTO.getValues())).build(
                    id,
                    schemaName,
                    new AjaxTextFieldPanel("panel", schemaName, new Model<String>()));
        }

        field.setEnabled(false);
        return field;
    }

}
