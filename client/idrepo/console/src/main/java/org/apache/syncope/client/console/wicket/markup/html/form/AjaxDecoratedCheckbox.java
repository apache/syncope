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
package org.apache.syncope.client.console.wicket.markup.html.form;

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;

/**
 * AjaxCheckBox allowing AjaxCallDecorator.
 */
public abstract class AjaxDecoratedCheckbox extends AjaxCheckBox {

    private static final long serialVersionUID = 7345848589265633002L;

    public AjaxDecoratedCheckbox(final String id) {
        this(id, null);
    }

    public AjaxDecoratedCheckbox(final String id, final IModel<Boolean> model) {
        super(id, model);

        add(new AjaxEventBehavior(Constants.ON_CLICK) {

            private static final long serialVersionUID = -295188647830294610L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                refreshComponent(target);
            }
        });
    }

    private void refreshComponent(final AjaxRequestTarget target) {
        target.add(this);
    }
}
