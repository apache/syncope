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
package org.apache.syncope.client.console.status;

import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

public class StatusModal<T extends AnyTO> extends Panel implements ModalPanel<AnyHandler<T>> {

    private static final long serialVersionUID = 1066124171682570080L;

    public StatusModal(
            final BaseModal<?> baseModal, final PageReference pageReference, final T anyTO, final boolean statusOnly) {
        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("status");
        add(mlp.setFirstLevel(new StatusDirectoryPanel(baseModal, mlp, pageReference, anyTO, statusOnly)));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AnyHandler<T> getItem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
