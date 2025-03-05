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
package org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table;

import org.apache.syncope.client.console.commons.ActionTableCheckGroup;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class CheckBoxPanel<T> extends Panel {

    private static final long serialVersionUID = 4062106303929176865L;

    private final Check<T> check;

    public CheckBoxPanel(final String componentId, final IModel<T> model, final CheckGroup<T> checkGroup) {
        super(componentId, model);
        this.check = new Check<>("check", model, checkGroup);
        if (checkGroup instanceof final ActionTableCheckGroup<T> components) {
            boolean checkable = components.isCheckable(model.getObject());
            this.check.setEnabled(checkable);
            this.check.setVisible(checkable);
        }
        add(this.check);
    }
}
