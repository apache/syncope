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
package org.syncope.console.pages;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.rest.SchemaRestClient;

/**
 * Modal window with Display attributes form.
 */
public class DisplayAttributesModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4274117450918385110L;

    /**
     * Max permitted selections.
     */
    private final int MAX_SELECTIONS = 9;

    private List<String> ATTRIBUTES_NOTINCLUDED = Arrays.asList(new String[]{
                "attributes", "derivedAttributes", "virtualAttributes",
                "serialVersionUID", "memberships", "resources", "password"});

    @SpringBean
    private PreferenceManager prefMan;

    /**
     * Schema rest client.
     */
    @SpringBean
    private SchemaRestClient schemaRestClient;

    final private IModel<List<String>> dnames =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID = 5275935387613157437L;

                @Override
                protected List<String> load() {

                    final List<String> details = new ArrayList<String>();

                    Class<?> clazz = UserTO.class;

                    // loop on class and all superclasses searching for field
                    while (clazz != null && clazz != Object.class) {
                        for (Field field : clazz.getDeclaredFields()) {
                            if (!ATTRIBUTES_NOTINCLUDED.contains(field.getName())) {
                                details.add(field.getName());
                            }
                        }
                        clazz = clazz.getSuperclass();
                    }

                    Collections.reverse(details);
                    return details;
                }
            };

    final private IModel<List<String>> names =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID =
                        5275935387613157437L;

                @Override
                protected List<String> load() {

                    List<String> schemas =
                            schemaRestClient.getSchemaNames("user");

                    if (schemas == null) {
                        schemas = new ArrayList<String>();
                    }

                    return schemas;
                }
            };

    final private IModel<List<String>> dsnames =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID =
                        5275935387613157437L;

                @Override
                protected List<String> load() {

                    List<String> schemas =
                            schemaRestClient.getDerivedSchemaNames("user");

                    if (schemas == null) {
                        schemas = new ArrayList<String>();
                    }

                    return schemas;
                }
            };

    final private IModel<List<String>> vsnames =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID =
                        5275935387613157437L;

                @Override
                protected List<String> load() {

                    List<String> schemas =
                            schemaRestClient.getVirtualSchemaNames("user");

                    if (schemas == null) {
                        schemas = new ArrayList<String>();
                    }

                    return schemas;
                }
            };

    private final List<String> userDetails;

    private final List<String> selectedSchemas;

    private final List<String> selectedVirSchemas;

    private final List<String> selectedDerSchemas;

    public DisplayAttributesModalPage(
            final PageReference callerPageRef,
            final ModalWindow window) {

        super();

        final Form form = new Form("form");
        form.setModel(new CompoundPropertyModel(this));

        userDetails = prefMan.getList(
                getRequest(), Constants.PREF_USERS_DETAILS_VIEW);

        selectedSchemas = prefMan.getList(
                getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW);

        selectedVirSchemas = prefMan.getList(
                getRequest(), Constants.PREF_USERS_VIRTUAL_ATTRIBUTES_VIEW);

        selectedDerSchemas = prefMan.getList(
                getRequest(), Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW);

        final CheckGroup dgroup = new CheckGroup(
                "dCheckGroup", new PropertyModel(this, "userDetails"));
        form.add(dgroup);

        final ListView<String> details =
                new ListView<String>("details", dnames) {

                    private static final long serialVersionUID = 9101744072914090143L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Check("dcheck", item.getModel()));
                        item.add(new Label("dname", new ResourceModel(
                                item.getModelObject(),
                                item.getModelObject())));
                    }
                };
        dgroup.add(details);

        if (names.getObject() != null && !names.getObject().isEmpty()) {
            final Fragment fragment =
                    new Fragment("schemas", "sfragment", form);
            form.add(fragment);

            final CheckGroup sgroup = new CheckGroup(
                    "sCheckGroup", new PropertyModel(this, "selectedSchemas"));
            fragment.add(sgroup);

            final ListView<String> schemas =
                    new ListView<String>("schemas", names) {

                        private static final long serialVersionUID = 9101744072914090143L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check("scheck", item.getModel()));
                            item.add(new Label("sname", new ResourceModel(
                                    item.getModelObject(),
                                    item.getModelObject())));
                        }
                    };
            sgroup.add(schemas);
        } else {
            final Fragment fragment =
                    new Fragment("schemas", "emptyFragment", form);
            form.add(fragment);

            selectedSchemas.clear();
        }

        if (vsnames.getObject() != null && !vsnames.getObject().isEmpty()) {
            final Fragment fragment =
                    new Fragment("vschemas", "vsfragment", form);
            form.add(fragment);

            final CheckGroup vsgroup = new CheckGroup(
                    "vsCheckGroup",
                    new PropertyModel(this, "selectedVirSchemas"));
            fragment.add(vsgroup);

            final ListView<String> virSchemas =
                    new ListView<String>("virSchemas", vsnames) {

                        private static final long serialVersionUID = 9101744072914090143L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check("vscheck", item.getModel()));
                            item.add(new Label("vsname", new ResourceModel(
                                    item.getModelObject(),
                                    item.getModelObject())));
                        }
                    };
            vsgroup.add(virSchemas);
        } else {
            final Fragment fragment =
                    new Fragment("vschemas", "emptyFragment", form);
            form.add(fragment);

            selectedVirSchemas.clear();
        }

        if (dsnames.getObject() != null && !dsnames.getObject().isEmpty()) {
            final Fragment fragment =
                    new Fragment("dschemas", "dsfragment", form);
            form.add(fragment);

            final CheckGroup dsgroup = new CheckGroup(
                    "dsCheckGroup",
                    new PropertyModel(this, "selectedDerSchemas"));
            fragment.add(dsgroup);

            final ListView<String> derSchemas =
                    new ListView<String>("derSchemas", dsnames) {

                        private static final long serialVersionUID = 9101744072914090143L;

                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Check("dscheck", item.getModel()));
                            item.add(new Label("dsname", new ResourceModel(
                                    item.getModelObject(),
                                    item.getModelObject())));
                        }
                    };
            dsgroup.add(derSchemas);
        } else {
            final Fragment fragment =
                    new Fragment("dschemas", "emptyFragment", form);
            form.add(fragment);

            selectedDerSchemas.clear();
        }

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "submit", new ResourceModel("submit")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                if (userDetails.size()
                        + selectedSchemas.size()
                        + selectedVirSchemas.size()
                        + selectedDerSchemas.size() > MAX_SELECTIONS) {
                    error(getString("tooMuchSelections"));
                    onError(target, form);
                } else {
                    final Map<String, List<String>> prefs =
                            new HashMap<String, List<String>>();

                    prefs.put(Constants.PREF_USERS_DETAILS_VIEW,
                            userDetails);

                    prefs.put(Constants.PREF_USERS_ATTRIBUTES_VIEW,
                            selectedSchemas);

                    prefs.put(Constants.PREF_USERS_VIRTUAL_ATTRIBUTES_VIEW,
                            selectedVirSchemas);

                    prefs.put(Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW,
                            selectedDerSchemas);

                    prefMan.setList(getRequest(), getResponse(), prefs);

                    ((BasePage) callerPageRef.getPage()).setModalResult(true);

                    window.close(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        form.add(submit);

        add(form);
    }
}
