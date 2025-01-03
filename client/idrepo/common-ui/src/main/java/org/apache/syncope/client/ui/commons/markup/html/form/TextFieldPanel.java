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

import java.util.Optional;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

public abstract class TextFieldPanel extends FieldPanel<String> {

    private static final long serialVersionUID = 1708195999215061362L;

    public TextFieldPanel(final String id, final String name, final IModel<String> model) {
        super(id, name, model);
    }

    protected TextFieldPanel setHTMLInputNotAllowed() {
        field.add(new IValidator<String>() {

            private static final long serialVersionUID = -8386207349500954732L;

            @Override
            public void validate(final IValidatable<String> validatable) {
                Optional.ofNullable(validatable.getValue()).filter(v -> v.indexOf('<') != -1).ifPresent(v -> {
                    ValidationError error = new ValidationError().addKey("htmlErrorMessage");
                    error.setVariable("label", field.getLabel().getObject());
                    validatable.error(error);
                });
            }
        });

        return this;
    }

    public void addValidator(final IValidator<? super String> validator) {
        this.field.add(validator);
    }
}
