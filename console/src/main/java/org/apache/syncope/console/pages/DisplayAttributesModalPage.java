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
package org.apache.syncope.console.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.search.SearchableFields;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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

/**
 * Modal window with Display attributes form.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DisplayAttributesModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4274117450918385110L;

    /**
     * Max allowed selections.
     */
    private static final int MAX_SELECTIONS = 9;

    public static final String[] DEFAULT_SELECTION = { "id", "username", "status" };

    @SpringBean
    private PreferenceManager prefMan;

    private final List<String> selectedDetails;

    private final List<String> selectedSchemas;

    private final List<String> selectedDerSchemas;

    public DisplayAttributesModalPage(final PageReference pageRef, final ModalWindow window,
            final List<String> schemaNames, final List<String> dSchemaNames) {

        super();

        final IModel<List<String>> fnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(UserTO.class);
            }
        };

        final IModel<List<String>> names = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return schemaNames;
            }
        };

        final IModel<List<String>> dsnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return dSchemaNames;
            }
        };

        final Form form = new Form(FORM);
        form.setModel(new CompoundPropertyModel(this));

        selectedDetails = prefMan.getList(getRequest(), Constants.PREF_USERS_DETAILS_VIEW);

        selectedSchemas = prefMan.getList(getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW);

        selectedDerSchemas = prefMan.getList(getRequest(), Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW);

        final CheckGroup dgroup = new CheckGroup("dCheckGroup", new PropertyModel(this, "selectedDetails"));
        form.add(dgroup);

        final ListView<String> details = new ListView<String>("details", fnames) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check("dcheck", item.getModel()));
                item.add(new Label("dname", new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };
        dgroup.add(details);

        if (names.getObject() == null || names.getObject().isEmpty()) {
            final Fragment fragment = new Fragment("schemas", "emptyFragment", form);
            form.add(fragment);

            selectedSchemas.clear();
        } else {
            final Fragment fragment = new Fragment("schemas", "sfragment", form);
            form.add(fragment);

            final CheckGroup sgroup = new CheckGroup("sCheckGroup", new PropertyModel(this, "selectedSchemas"));
            fragment.add(sgroup);

            final ListView<String> schemas = new ListView<String>("schemas", names) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check("scheck", item.getModel()));
                    item.add(new Label("sname", new ResourceModel(item.getModelObject(), item.getModelObject())));
                }
            };
            sgroup.add(schemas);
        }

        if (dsnames.getObject() == null || dsnames.getObject().isEmpty()) {
            final Fragment fragment = new Fragment("dschemas", "emptyFragment", form);
            form.add(fragment);

            selectedDerSchemas.clear();
        } else {
            final Fragment fragment = new Fragment("dschemas", "dsfragment", form);
            form.add(fragment);

            final CheckGroup dsgroup = new CheckGroup("dsCheckGroup", new PropertyModel(this, "selectedDerSchemas"));
            fragment.add(dsgroup);

            final ListView<String> derSchemas = new ListView<String>("derSchemas", dsnames) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check("dscheck", item.getModel()));
                    item.add(new Label("dsname", new ResourceModel(item.getModelObject(), item.getModelObject())));
                }
            };
            dsgroup.add(derSchemas);
        }

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (selectedDetails.size() + selectedSchemas.size() + selectedDerSchemas.size()
                        > MAX_SELECTIONS) {

                    error(getString("tooManySelections"));
                    onError(target, form);
                } else {
                    final Map<String, List<String>> prefs = new HashMap<String, List<String>>();

                    prefs.put(Constants.PREF_USERS_DETAILS_VIEW, selectedDetails);

                    prefs.put(Constants.PREF_USERS_ATTRIBUTES_VIEW, selectedSchemas);

                    prefs.put(Constants.PREF_USERS_DERIVED_ATTRIBUTES_VIEW, selectedDerSchemas);

                    prefMan.setList(getRequest(), getResponse(), prefs);

                    ((BasePage) pageRef.getPage()).setModalResult(true);

                    window.close(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };

        form.add(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }
}
