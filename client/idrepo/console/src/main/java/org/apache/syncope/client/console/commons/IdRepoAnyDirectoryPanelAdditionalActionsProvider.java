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
package org.apache.syncope.client.console.commons;

import java.util.List;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;

public class IdRepoAnyDirectoryPanelAdditionalActionsProvider implements AnyDirectoryPanelAdditionalActionsProvider {

    private static final long serialVersionUID = -6768727277642238924L;

    @Override
    public void add(
            final AnyDirectoryPanel<?, ?> panel,
            final BaseModal<?> modal,
            final boolean wizardInModal,
            final WebMarkupContainer container,
            final String type,
            final String realm,
            final String fiql,
            final int rows,
            final List<String> pSchemaNames,
            final List<String> dSchemaNames,
            final PageReference pageRef) {

        panel.addInnerObject(new AjaxLink<Void>("csvPush") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                // nothing to do
            }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false).setEnabled(false));
        panel.addInnerObject(new AjaxLink<Void>("csvPull") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                // nothing to do
            }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false).setEnabled(false));
    }

    @Override
    public void hide() {
        // nothing to do
    }
}
