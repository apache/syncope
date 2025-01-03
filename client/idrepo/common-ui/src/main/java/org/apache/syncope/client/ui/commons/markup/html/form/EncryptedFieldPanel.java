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
package org.apache.syncope.client.ui.commons.markup.html.form;

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class EncryptedFieldPanel extends TextFieldPanel implements Cloneable {

    private static final long serialVersionUID = 1882871043451691005L;

    public EncryptedFieldPanel(final String id, final String name, final IModel<String> model) {
        this(id, name, model, false);
    }

    public EncryptedFieldPanel(
            final String id,
            final String name,
            final IModel<String> model,
            final boolean enableOnChange) {

        super(id, name, model);

        field = new TextField<>("encryptedField", model) {

            private static final long serialVersionUID = 7545877620091912863L;

            @Override
            protected String[] getInputTypes() {
                return new String[] { "password" };
            }
        };
        setHTMLInputNotAllowed();

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        add(field.setLabel(new ResourceModel(name, name)).setRequired(false).setOutputMarkupId(true));
    }
}
