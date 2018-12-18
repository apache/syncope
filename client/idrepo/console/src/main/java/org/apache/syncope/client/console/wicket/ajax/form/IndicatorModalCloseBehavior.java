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
package org.apache.syncope.client.console.wicket.ajax.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;

/**
 * An {@link AjaxEventBehavior} not show in veil.
 */
public abstract class IndicatorModalCloseBehavior extends AjaxEventBehavior implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -5144403874783384604L;

    private final String indicator;

    /**
     * Constructor.
     */
    public IndicatorModalCloseBehavior() {
        this(StringUtils.EMPTY);
    }

    /**
     * Constructor.
     *
     * @param indicator indicator.
     */
    public IndicatorModalCloseBehavior(final String indicator) {
        super("hidden.bs.modal");
        this.indicator = indicator;
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return indicator;
    }
}
