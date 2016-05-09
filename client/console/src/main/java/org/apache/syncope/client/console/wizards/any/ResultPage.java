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
package org.apache.syncope.client.console.wizards.any;

import java.io.Serializable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.syncope.client.console.panels.WizardModalPanel;

public abstract class ResultPage<T extends Serializable> extends Panel implements WizardModalPanel<T> {

    private static final long serialVersionUID = -1619945285130369086L;

    private final T item;

    public ResultPage(final T item, final Serializable result) {
        super(BaseModal.CONTENT_ID);
        setOutputMarkupId(true);
        this.item = item;

        add(customResultBody("customResultBody", item, result));

        add(ActionLinksPanel.<T>builder().add(new ActionLink<T>() {

            private static final long serialVersionUID = 3257738274365467945L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                closeAction(target);
            }
        }, ActionLink.ActionType.CLOSE).build("action").setRenderBodyOnly(true));
    }

    protected abstract void closeAction(final AjaxRequestTarget target);

    protected abstract Panel customResultBody(final String panleId, final T item, final Serializable result);

    @Override
    public T getItem() {
        return this.item;
    }
}
