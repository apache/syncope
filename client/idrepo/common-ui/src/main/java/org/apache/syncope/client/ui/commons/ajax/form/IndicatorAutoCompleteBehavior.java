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
package org.apache.syncope.client.ui.commons.ajax.form;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;

/**
 * An {@link AutoCompleteBehavior} not show in veil.
 *
 * @param <T> element type.
 */
public abstract class IndicatorAutoCompleteBehavior<T extends Serializable>
        extends AutoCompleteBehavior<T> implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -5144403874783384604L;

    private final String indicator;

    public IndicatorAutoCompleteBehavior(final IAutoCompleteRenderer<T> renderer, final AutoCompleteSettings settings) {
        this(renderer, settings, StringUtils.EMPTY);
    }

    public IndicatorAutoCompleteBehavior(
            final IAutoCompleteRenderer<T> renderer,
            final AutoCompleteSettings settings,
            final String indicator) {
        super(renderer, settings);
        this.indicator = indicator;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return indicator;
    }
}
