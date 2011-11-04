/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.List;
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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;

/**
 * Modal window with Display attributes form.
 */
public class DisplayAttributesModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4274117450918385110L;

    private final int MAX_SELECTIONS = 10;

    @SpringBean
    private PreferenceManager prefMan;

    private final List<String> selectedSchemas;

    public DisplayAttributesModalPage(
            final PageReference callerPageRef,
            final IModel<List<String>> names,
            final ModalWindow window) {

        super();

        final Form form = new Form("UserAttributesForm");
        form.setModel(new CompoundPropertyModel(this));
        selectedSchemas = prefMan.getList(
                getRequest(), Constants.PREF_USERS_ATTRIBUTES_VIEW);

        final CheckGroup group = new CheckGroup(
                "checkgroup", new PropertyModel(this, "selectedSchemas"));
        form.add(group);

        final ListView<String> schemas =
                new ListView<String>("schemas", names) {

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Check("check", item.getModel()));
                        item.add(new Label("name", item.getModelObject()));
                    }
                };
        group.add(schemas);

        group.add(new AbstractValidator() {

            @Override
            protected void onValidate(IValidatable iv) {
                if (((List) iv.getValue()).size() > MAX_SELECTIONS) {
                    error(iv, "tooMuchSelections");
                }
            }
        });

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "submit",
                new ResourceModel("submit")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                prefMan.setList(getRequest(), getResponse(),
                        Constants.PREF_USERS_ATTRIBUTES_VIEW,
                        selectedSchemas);
                ((Users) callerPageRef.getPage()).setModalResult(true);
                window.close(target);
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
