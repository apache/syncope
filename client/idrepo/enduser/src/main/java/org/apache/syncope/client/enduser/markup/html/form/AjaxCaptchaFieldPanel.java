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
package org.apache.syncope.client.enduser.markup.html.form;

import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;

public class AjaxCaptchaFieldPanel extends Panel {

    private static final long serialVersionUID = 238940918106696068L;

    private RequiredTextField<String> field;

    public AjaxCaptchaFieldPanel(final String id, final String name, final IModel<String> model) {
        this(id, name, model, true);
    }

    public AjaxCaptchaFieldPanel(
            final String id, final String name, final IModel<String> model, final boolean enableOnChange) {

        super(id, model);

        field = new RequiredTextField<String>("textField", model, String.class) {

            private static final long serialVersionUID = -8751221833502425836L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                // clear the field after each render
                tag.put("value", "");
            }
        };
        field.setLabel(new ResourceModel(name, name));

        add(field.setOutputMarkupId(true));

        if (enableOnChange && !field.isEnabled()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }
    }

    public void addValidator(final IValidator<? super String> validator) {
        this.field.add(validator);
    }

}
