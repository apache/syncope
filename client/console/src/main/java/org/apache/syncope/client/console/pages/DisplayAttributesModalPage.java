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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
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

/**
 * Modal window with Display attributes form.
 *
 * @param <T> anyTO
 */
public abstract class DisplayAttributesModalPage<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = -4274117450918385110L;

    /**
     * Max allowed selections.
     */
    private static final int MAX_SELECTIONS = 9;

    private final PreferenceManager prefMan = new PreferenceManager();

    private final List<String> selectedDetails;

    private final List<String> selectedPlainSchemas;

    private final List<String> selectedDerSchemas;

    public DisplayAttributesModalPage(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> schemaNames,
            final List<String> dSchemaNames) {

        super(modal, pageRef);

        final IModel<List<String>> fnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(getTOClass());
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

        final Form<DisplayAttributesModalPage<T>> form = new Form<>(FORM);
        form.setModel(new CompoundPropertyModel<>(this));

        selectedDetails = prefMan.getList(getRequest(), getPrefDetailView());

        selectedPlainSchemas = prefMan.getList(getRequest(), getPrefAttributeView());

        selectedDerSchemas = prefMan.getList(getRequest(), getPrefDerivedAttributeView());

        final CheckGroup<String> dgroup
                = new CheckGroup<>("dCheckGroup", new PropertyModel<List<String>>(this, "selectedDetails"));
        form.add(dgroup);

        final ListView<String> details = new ListView<String>("details", fnames) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check<>("dcheck", item.getModel()));
                item.add(new Label("dname", new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };
        dgroup.add(details);

        if (names.getObject() == null || names.getObject().isEmpty()) {
            final Fragment fragment = new Fragment("plainSchemas", "emptyFragment", form);
            form.add(fragment);

            selectedPlainSchemas.clear();
        } else {
            final Fragment fragment = new Fragment("plainSchemas", "sfragment", form);
            form.add(fragment);

            final CheckGroup<String> sgroup
                    = new CheckGroup<>("psCheckGroup", new PropertyModel<List<String>>(this, "selectedPlainSchemas"));
            fragment.add(sgroup);

            final ListView<String> schemas = new ListView<String>("plainSchemas", names) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(final ListItem<String> item) {
                    item.add(new Check<>("scheck", item.getModel()));
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

            final CheckGroup<String> dsgroup
                    = new CheckGroup<>("dsCheckGroup", new PropertyModel<List<String>>(this, "selectedDerSchemas"));
            fragment.add(dsgroup);

            final ListView<String> derSchemas = new ListView<String>("derSchemas", dsnames) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(final ListItem<String> item) {
                    item.add(new Check<>("dscheck", item.getModel()));
                    item.add(new Label("dsname", new ResourceModel(item.getModelObject(), item.getModelObject())));
                }
            };
            dsgroup.add(derSchemas);
        }

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                if (selectedDetails.size() + selectedPlainSchemas.size() + selectedDerSchemas.size()
                        > MAX_SELECTIONS) {

                    error(getString("tooManySelections"));
                    onError(target, form);
                } else {
                    final Map<String, List<String>> prefs = new HashMap<>();

                    prefs.put(getPrefDetailView(), selectedDetails);

                    prefs.put(getPrefAttributeView(), selectedPlainSchemas);

                    prefs.put(getPrefDerivedAttributeView(), selectedDerSchemas);

                    prefMan.setList(getRequest(), getResponse(), prefs);

                    info(getString(Constants.OPERATION_SUCCEEDED));

                    modal.close(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                modal.getNotificationPanel().refresh(target);
            }
        };

        form.add(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                modal.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }

    public abstract String getPrefDetailView();

    public abstract String getPrefAttributeView();

    public abstract String getPrefDerivedAttributeView();

    public abstract Class<? extends AnyTO> getTOClass();

}
