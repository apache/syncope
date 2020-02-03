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
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class AuditHistoryModal<T extends EntityTO> extends Panel implements ModalPanel {

    private static final long serialVersionUID = 1066124171682570080L;

    public AuditHistoryModal(
            final BaseModal<?> baseModal,
            final AuditElements.EventCategoryType type,
            final String category,
            final T entity,
            final String auditRestoreEntitlement,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID);

        MultilevelPanel mlp = new MultilevelPanel("history");
        mlp.setOutputMarkupId(true);
        add(mlp.setFirstLevel(new AuditHistoryDirectoryPanel<T>(
                baseModal,
                mlp,
                type,
                category,
                entity,
                auditRestoreEntitlement,
                pageRef) {

            private static final long serialVersionUID = 1952220682903768286L;

            @Override
            protected void restore(final String json, final AjaxRequestTarget target) {
                AuditHistoryModal.this.restore(json, target);
            }
        }));
    }

    protected abstract void restore(String json, AjaxRequestTarget target);
}
