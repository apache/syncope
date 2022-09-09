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
package org.apache.syncope.client.ui.commons.wizards.any;

import java.io.Serializable;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class AbstractResultPanel<T extends Serializable, R extends Serializable> extends Panel
        implements WizardModalPanel<T> {

    private static final long serialVersionUID = -1619945285130369086L;

    protected final T item;

    protected final R result;

    public AbstractResultPanel(final T item, final R result) {
        super(Constants.CONTENT_ID);
        setOutputMarkupId(true);
        this.item = item;
        this.result = result;

        add(customResultBody("customResultBody", item, result).setOutputMarkupId(true));
    }

    protected abstract Panel customResultBody(String panelId, T item, R result);

    @Override
    public T getItem() {
        return this.item;
    }

    public R getResult() {
        return result;
    }
}
