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

import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class StatusModal<T extends EntityTO> extends Panel implements ModalPanel {

    private static final long serialVersionUID = 1066124171682570080L;

    protected final DirectoryPanel<StatusBean, StatusBean, ?, ?> directoryPanel;

    public StatusModal(
            final PageReference pageReference,
            final T entity,
            final String itemKeyFieldName,
            final boolean statusOnly) {

        super(BaseModal.CONTENT_ID);

        MultilevelPanel mlp = new MultilevelPanel("status");
        mlp.setOutputMarkupId(true);
        directoryPanel = getStatusDirectoryPanel(mlp, pageReference, entity, itemKeyFieldName, statusOnly);
        add(mlp.setFirstLevel(directoryPanel));
    }

    protected abstract DirectoryPanel<
        StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?>> getStatusDirectoryPanel(
            MultilevelPanel mlp,
            PageReference pageReference,
            T entity,
            String itemKeyFieldName,
            boolean statusOnly);
}
