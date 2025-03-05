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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
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

    public ConnObjectPanel(
            final String id,
            final Pair<IModel<?>, IModel<?>> titles,
            final Pair<ConnObject, ConnObject> connObjectTOs,
            final boolean hideLeft) {

        super(id);

        final IModel<List<String>> formProps = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<Attr> right = new ArrayList<>(connObjectTOs == null || connObjectTOs.getRight() == null
                    ? List.of()
                    : connObjectTOs.getRight().getAttrs());
                List<Attr> left = new ArrayList<>(connObjectTOs == null || connObjectTOs.getLeft() == null
                    ? List.of()
                    : connObjectTOs.getLeft().getAttrs());

                List<String> schemas = ListUtils.sum(right.stream().map(Attr::getSchema).collect(Collectors.toList()),
                    left.stream().map(Attr::getSchema).collect(Collectors.toList()));
                Collections.sort(schemas);
                return schemas;
            }
        };

        add(new Label("leftTitle", titles.getLeft()).setOutputMarkupPlaceholderTag(true).setVisible(!hideLeft));
        add(new Label("rightTitle", titles.getRight()));

        final Map<String, Attr> leftProfile = connObjectTOs == null || connObjectTOs.getLeft() == null
                ? null
                : EntityTOUtils.buildAttrMap(connObjectTOs.getLeft().getAttrs());
        final Map<String, Attr> rightProfile = connObjectTOs == null || connObjectTOs.getRight() == null
                ? null
                : EntityTOUtils.buildAttrMap(connObjectTOs.getRight().getAttrs());
        ListView<String> propView = new ListView<>("propView", formProps) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String prop = item.getModelObject();

                final Fragment valueFragment;
                final Attr left = Optional.ofNullable(leftProfile)
                    .map(stringAttrMap -> stringAttrMap.get(prop)).orElse(null);
                final Attr right = Optional.ofNullable(rightProfile)
                    .map(profile -> profile.get(prop)).orElse(null);

                valueFragment = new Fragment("value", "doubleValue", ConnObjectPanel.this);
                valueFragment.add(getValuePanel("leftAttribute", prop, left).
                    setOutputMarkupPlaceholderTag(true).setVisible(!hideLeft));
                valueFragment.add(getValuePanel("rightAttribute", prop, right));

                if (left == null || right == null
                    || (CollectionUtils.isNotEmpty(right.getValues())
                    && CollectionUtils.isEmpty(left.getValues()))
                    || (CollectionUtils.isEmpty(right.getValues())
                    && CollectionUtils.isNotEmpty(left.getValues()))
                    || (CollectionUtils.isNotEmpty(right.getValues())
                    && CollectionUtils.isNotEmpty(left.getValues())
                    && right.getValues().size() != left.getValues().size())
                    || (CollectionUtils.isNotEmpty(right.getValues())
                    && CollectionUtils.isNotEmpty(left.getValues())
                    && !right.getValues().equals(left.getValues()))) {

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
    private static Panel getValuePanel(final String id, final String schemaName, final Attr attrTO) {
        Panel field;
        if (attrTO == null) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>());
        } else if (CollectionUtils.isEmpty(attrTO.getValues())) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>());
        } else if (ConnIdSpecialName.PASSWORD.equals(schemaName)) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>("********"));
        } else if (attrTO.getValues().size() == 1) {
            field = new AjaxTextFieldPanel(id, schemaName, new Model<>(attrTO.getValues().getFirst()));
        } else {
            field = new MultiFieldPanel.Builder<>(new ListModel<>(attrTO.getValues())).build(
                    id,
                    schemaName,
                    new AjaxTextFieldPanel("panel", schemaName, new Model<>()));
        }

        field.setEnabled(false);
        return field;
    }
}
