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
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
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

    public ConnObjectPanel(final String id, final Pair<ConnObjectTO, ConnObjectTO> connObjectTOs, final boolean view) {
        super(id);

        final IModel<List<String>> formProps = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<AttrTO> right = new ArrayList<>(connObjectTOs == null || connObjectTOs.getRight() == null
                        ? Collections.<AttrTO>emptyList()
                        : connObjectTOs.getRight().getAttrs());

                List<AttrTO> left = new ArrayList<>(connObjectTOs == null || connObjectTOs.getLeft() == null
                        ? Collections.<AttrTO>emptyList()
                        : connObjectTOs.getLeft().getAttrs());

                final List<String> schemas = ListUtils.sum(
                        CollectionUtils.collect(right, new Transformer<AttrTO, String>() {

                            @Override
                            public String transform(final AttrTO input) {
                                return input.getSchema();
                            }
                        }, new ArrayList<String>()),
                        CollectionUtils.collect(left, new Transformer<AttrTO, String>() {

                            @Override
                            public String transform(final AttrTO input) {
                                return input.getSchema();
                            }
                        }, new ArrayList<String>()));

                Collections.sort(schemas);

                return schemas;
            }
        };

        final Map<String, AttrTO> beforeProfile = connObjectTOs == null || connObjectTOs.getLeft() == null
                ? null
                : connObjectTOs.getLeft().getAttrMap();

        final Map<String, AttrTO> afterProfile = connObjectTOs == null || connObjectTOs.getRight() == null
                ? null
                : connObjectTOs.getRight().getAttrMap();

        final ListView<String> propView = new ListView<String>("propView", formProps) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String prop = item.getModelObject();

                final Fragment valueFragment;
                final AttrTO before = beforeProfile == null ? null : beforeProfile.get(prop);
                final AttrTO after = afterProfile == null ? null : afterProfile.get(prop);

                valueFragment = new Fragment("value", "doubleValue", ConnObjectPanel.this);

                Panel oldAttribute = getValuePanel("oldAttribute", prop, before);
                oldAttribute.setOutputMarkupPlaceholderTag(true);
                oldAttribute.setVisible(!view);
                valueFragment.add(oldAttribute);

                valueFragment.add(getValuePanel("newAttribute", prop, after));

                if (before == null || after == null
                        || (CollectionUtils.isNotEmpty(after.getValues())
                        && CollectionUtils.isEmpty(before.getValues()))
                        || (CollectionUtils.isEmpty(after.getValues())
                        && CollectionUtils.isNotEmpty(before.getValues()))
                        || (CollectionUtils.isNotEmpty(after.getValues())
                        && CollectionUtils.isNotEmpty(before.getValues())
                        && after.getValues().size() != before.getValues().size())
                        || (CollectionUtils.isNotEmpty(after.getValues())
                        && CollectionUtils.isNotEmpty(before.getValues())
                        && !after.getValues().equals(before.getValues()))) {
                    valueFragment.add(new Behavior() {

                        private static final long serialVersionUID = 3109256773218160485L;

                        @Override
                        public void onComponentTag(final Component component, final ComponentTag tag) {
                            tag.put("class", "highlight");
                        }
                    });
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
        } else if (ConnIdSpecialName.PASSWORD.equals(schemaName)) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>("********"));
        } else if (attrTO.getValues().size() == 1) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>(attrTO.getValues().get(0)));
        } else {
            field = new MultiFieldPanel.Builder<>(new ListModel<>(attrTO.getValues())).build(
                    id,
                    schemaName,
                    new AjaxTextFieldPanel("panel", schemaName, new Model<String>()));
        }

        field.setEnabled(false);
        return field;
    }

}
