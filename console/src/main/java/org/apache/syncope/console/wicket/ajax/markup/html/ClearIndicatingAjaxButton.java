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
package org.apache.syncope.console.wicket.ajax.markup.html;

import org.apache.syncope.console.pages.BasePage;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public abstract class ClearIndicatingAjaxButton extends IndicatingAjaxButton {

    private static final long serialVersionUID = 7206379812788748287L;

    private final PageReference pageRef;

    public ClearIndicatingAjaxButton(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
    }

    public ClearIndicatingAjaxButton(final String id, Form<?> form, final PageReference pageRef) {
        super(id, form);
        this.pageRef = pageRef;
    }

    public ClearIndicatingAjaxButton(final String id, IModel<String> model, final PageReference pageRef) {
        super(id, model);
        this.pageRef = pageRef;
    }

    public ClearIndicatingAjaxButton(final String id, final IModel<String> model, final Form<?> form,
            final PageReference pageRef) {

        super(id, model, form);
        this.pageRef = pageRef;
    }

    protected abstract void onSubmitInternal(AjaxRequestTarget target, Form<?> form);

    @Override
    protected final void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        super.onSubmit(target, form);

        Page page = pageRef.getPage();
        if (page instanceof BasePage) {
            target.add(((BasePage) page).getFeedbackPanel());
        }
        onSubmitInternal(target, form);
    }
}
