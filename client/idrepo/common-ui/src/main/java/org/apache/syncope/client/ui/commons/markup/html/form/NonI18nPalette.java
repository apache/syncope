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

import java.util.Collection;
import java.util.Map;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public class NonI18nPalette<T> extends Palette<T> {

    private static final long serialVersionUID = 2659070187837941889L;

    public NonI18nPalette(final String id,
            final IModel<? extends Collection<T>> model,
            final IModel<? extends Collection<? extends T>> choicesModel,
            final IChoiceRenderer<? super T> choiceRenderer, final int rows,
            final boolean allowOrder, final boolean allowMoveAll) {

        super(id, model, choicesModel, choiceRenderer, rows, allowOrder, allowMoveAll);
    }

    @Override
    protected boolean localizeDisplayValues() {
        return false;
    }

    protected Map<String, String> getAdditionalAttributes(final Object choice) {
        return Map.of("title", choice.toString());
    }

    @Override
    protected Map<String, String> getAdditionalAttributesForChoices(final Object choice) {
        return getAdditionalAttributes(choice);
    }

    @Override
    protected Map<String, String> getAdditionalAttributesForSelection(final Object choice) {
        return getAdditionalAttributes(choice);
    }
}
