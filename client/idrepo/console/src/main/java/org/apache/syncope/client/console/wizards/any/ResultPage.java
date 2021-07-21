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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;

public abstract class ResultPage<T extends Serializable> extends Panel implements WizardModalPanel<T> {

    private static final long serialVersionUID = -1619945285130369086L;

    private final T item;

    private final Serializable result;

    public ResultPage(final T item, final Serializable result) {
        super(BaseModal.CONTENT_ID);
        setOutputMarkupId(true);
        this.item = item;
        this.result = result;

        add(customResultBody("customResultBody", item, result));

        ActionsPanel<T> panel = new ActionsPanel<>(Constants.ACTION, null);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 3257738274365467945L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                closeAction(target);
            }
        }, ActionLink.ActionType.CLOSE, StringUtils.EMPTY).hideLabel();
        add(panel.setRenderBodyOnly(true));
    }

    protected abstract void closeAction(AjaxRequestTarget target);

    protected abstract Panel customResultBody(String panleId, T item, Serializable result);

    @Override
    public T getItem() {
        return this.item;
    }

    public Serializable getResult() {
        return result;
    }
}
