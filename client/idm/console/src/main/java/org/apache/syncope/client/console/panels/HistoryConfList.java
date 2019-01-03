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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ResourceHistoryConfTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

public class HistoryConfList<T extends Serializable> extends Panel implements ModalPanel {

    private static final long serialVersionUID = 2482507052037665907L;

    public <T extends AnyTO> HistoryConfList(
            final BaseModal<?> baseModal,
            final String entityKey,
            final PageReference pageReference,
            final EntityTO modelObj) {

        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("history");

        mlp.setFirstLevel(modelObj instanceof ConnInstanceTO
                ? new ConnInstanceHistoryConfDirectoryPanel(baseModal, mlp, entityKey, pageReference) {

            private static final long serialVersionUID = 1422189028000709100L;

            @Override
            protected void viewConfiguration(final ConnInstanceHistoryConfTO historyTO,
                    final AjaxRequestTarget target) {
                mlp.next(
                        new StringResourceModel("history.diff.view", this).getObject(),
                        new HistoryConfDetails<>(modal, historyTO, pageReference, restClient.list(entityKey)), target);
            }
        } : new ResourceHistoryConfDirectoryPanel(baseModal, mlp, entityKey, pageReference) {

            private static final long serialVersionUID = 1422189028000709100L;

            @Override
            protected void viewConfiguration(final ResourceHistoryConfTO historyTO,
                    final AjaxRequestTarget target) {
                mlp.next(
                        new StringResourceModel("history.diff.view", this).getObject(),
                        new HistoryConfDetails<>(modal, historyTO, pageReference, restClient.list(entityKey)), target);
            }
        });

        add(mlp);
    }
}
