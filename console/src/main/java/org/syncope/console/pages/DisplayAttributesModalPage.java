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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;

/**
 * Modal window with Display attributes form.
 */
public class DisplayAttributesModalPage extends BaseModalPage {

    @SpringBean
    private PreferenceManager prefMan;

    private final List<String> selectedSchemas;

    public DisplayAttributesModalPage(final Users basePage,
            final IModel<List<String>> schemaNames,
            final ModalWindow window) {

        super();

        Form userAttributesForm = new Form("UserAttributesForm");
        userAttributesForm.setModel(new CompoundPropertyModel(this));
        selectedSchemas = prefMan.getList(getWebRequestCycle().getWebRequest(),
                Constants.PREF_USERS_ATTRIBUTES_VIEW);

        userAttributesForm.add(new CheckBoxMultipleChoice("schemaNames",
                new PropertyModel(this, "selectedSchemas"), schemaNames));
        IndicatingAjaxButton submit = new IndicatingAjaxButton("submit",
                new Model(getString("submit"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                prefMan.setList(getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_USERS_ATTRIBUTES_VIEW, selectedSchemas);
                basePage.setModalResult(true);
                window.close(target);
            }
        };
        userAttributesForm.add(submit);
        add(userAttributesForm);
    }
}
