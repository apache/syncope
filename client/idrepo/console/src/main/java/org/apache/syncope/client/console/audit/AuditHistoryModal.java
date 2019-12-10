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
package org.apache.syncope.client.console.audit;

import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.panel.Panel;

public class AuditHistoryModal<T extends AnyTO> extends Panel implements ModalPanel {

    private static final long serialVersionUID = 1066124171682570080L;

    protected final AuditHistoryDirectoryPanel directoryPanel;

    public AuditHistoryModal(
            final BaseModal<?> baseModal,
            final PageReference pageReference,
            final T entity) {

        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("history");
        mlp.setOutputMarkupId(true);
        this.directoryPanel = getDirectoryPanel(mlp, baseModal, pageReference, entity);
        add(mlp.setFirstLevel(this.directoryPanel));
    }

    protected AuditHistoryDirectoryPanel getDirectoryPanel(
            final MultilevelPanel mlp,
            final BaseModal<?> baseModal,
            final PageReference pageReference,
            final T entity) {

        return new AuditHistoryDirectoryPanel(baseModal, mlp, pageReference, entity);
    }
}
