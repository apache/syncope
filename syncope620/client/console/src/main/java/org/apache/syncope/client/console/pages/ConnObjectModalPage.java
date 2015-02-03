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
package org.apache.syncope.client.console.pages;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.list.AltListView;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class ConnObjectModalPage extends BaseModalPage {

    private static final long serialVersionUID = -6469290753080058487L;

    public ConnObjectModalPage(final ConnObjectTO connObjectTO) {
        super();

        final Form<Void> form = new Form<Void>(FORM);
        form.setEnabled(false);
        add(form);

        IModel<List<AttrTO>> formProps = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AttrTO> load() {
                List<AttrTO> attrs = connObjectTO.getPlainAttrs();
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
        final ListView<AttrTO> propView = new AltListView<AttrTO>("propView", formProps) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void populateItem(final ListItem<AttrTO> item) {
                final AttrTO prop = item.getModelObject();

                Label label = new Label("key", prop.getSchema());
                item.add(label);

                Panel field;
                if (prop.getValues().isEmpty()) {
                    field = new AjaxTextFieldPanel("value",
                            prop.getSchema(), new Model<String>());
                } else if (prop.getValues().size() == 1) {
                    field = new AjaxTextFieldPanel("value",
                            prop.getSchema(), new Model<String>(prop.getValues().get(0)));
                } else {
                    field = new MultiFieldPanel<String>("value", new ListModel<String>(prop.getValues()),
                            new AjaxTextFieldPanel("panel", prop.getSchema(), new Model<String>()));
                }
                item.add(field);
            }
        };
        form.add(propView);
    }
}
