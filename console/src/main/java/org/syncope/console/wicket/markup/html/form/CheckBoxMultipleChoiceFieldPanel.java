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
package org.syncope.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.model.IModel;

public class CheckBoxMultipleChoiceFieldPanel extends AbstractFieldPanel {

    private static final long serialVersionUID = 4124935025837737298L;

    private final CheckBoxMultipleChoice field;

    public CheckBoxMultipleChoiceFieldPanel(final String id, final IModel<Collection> model,
            final IModel<List> choices) {

        super(id, model);

        field = new CheckBoxMultipleChoice("checkBoxMultipleChoice", model, choices);
        add(field);
    }

    @Override
    public AbstractFieldPanel setModelObject(final Serializable object) {
        field.setModelObject(object);
        return this;
    }
}
