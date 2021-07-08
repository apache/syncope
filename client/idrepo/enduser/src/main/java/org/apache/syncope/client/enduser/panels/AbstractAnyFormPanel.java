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
package org.apache.syncope.client.enduser.panels;

import java.io.Serializable;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;

public abstract class AbstractAnyFormPanel<T extends Serializable> extends AbstractFormPanel<T> {

    private static final long serialVersionUID = -5976166731584959275L;

    protected final Form<T> form;

    public AbstractAnyFormPanel(final String id, final T defaultItem, final PageReference pageReference) {
        super(id, defaultItem, pageReference);

        form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);
        AjaxButton submitButton = new AjaxButton("submit") {

            private static final long serialVersionUID = 4284361595033427185L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                onFormSubmit(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BasePage) getPage()).getNotificationPanel().refresh(target);
            }
        };

        submitButton.setOutputMarkupId(true);
        submitButton.setDefaultFormProcessing(true);
        form.add(submitButton);

        Button cancel = new Button("cancel") {

            private static final long serialVersionUID = 3669569969172391336L;

            @Override
            public void onSubmit() {
                setResponsePage(getApplication().getHomePage());
            }

        };
        cancel.setOutputMarkupId(true);
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }

    public Form<T> getForm() {
        return form;
    }

    public void setFormModel(final T modelObject) {
        form.setModel(new CompoundPropertyModel<>(modelObject));
    }

    protected void onCancelInternal(final T modelObject) {
    }

    protected Serializable onApplyInternal(final T modelObject) {
        // do nothing
        return null;
    }

    protected abstract void buildLayout(T modelObject);

    protected abstract void onFormSubmit(AjaxRequestTarget target);
}
