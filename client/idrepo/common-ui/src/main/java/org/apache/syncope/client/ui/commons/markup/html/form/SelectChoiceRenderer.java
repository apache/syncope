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

import java.util.List;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public class SelectChoiceRenderer<T> implements IChoiceRenderer<T> {

    private static final long serialVersionUID = -3242441544405909243L;

    @Override
    public Object getDisplayValue(final T obj) {
        if (obj instanceof final SelectOption selectOption) {
            return selectOption.getDisplayValue();
        } else {
            return obj.toString();
        }
    }

    @Override
    public String getIdValue(final T obj, final int i) {
        return obj.toString();
    }

    @Override
    public T getObject(final String id, final IModel<? extends List<? extends T>> choices) {
        return choices.getObject().stream().
                filter(object -> id != null && id.equals(getIdValue(object, 0))).
                findAny().orElse(null);
    }
}
