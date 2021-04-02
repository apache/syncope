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
package org.apache.syncope.client.console.policies;

import java.util.List;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.panels.AttrListDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.model.IModel;

public class AccessPolicyRequiredAttrsDirectoryPanel extends AttrListDirectoryPanel {

    private static final long serialVersionUID = 1L;

    private final BaseModal<AccessPolicyTO> wizardModal;

    private final IModel<AccessPolicyTO> model;

    public AccessPolicyRequiredAttrsDirectoryPanel(
            final String id,
            final BaseModal<AccessPolicyTO> wizardModal,
            final IModel<AccessPolicyTO> model,
            final PageReference pageRef) {

        super(id, pageRef, false);

        this.wizardModal = wizardModal;
        this.model = model;

        setOutputMarkupId(true);

        enableUtilityButton();
        setFooterVisibility(false);

        addNewItemPanelBuilder(
                new AccessPolicyRequiredAttrsWizardBuilder(model.getObject(), new Attr(), pageRef), true);

        initResultTable();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            wizardModal.close(target);
        } else if (event.getPayload() instanceof AjaxWizard.EditItemActionEvent) {
            @SuppressWarnings("unchecked")
            AjaxWizard.EditItemActionEvent<?> payload = (AjaxWizard.EditItemActionEvent<?>) event.getPayload();
            payload.getTarget().ifPresent(actionTogglePanel::close);
        }
        super.onEvent(event);
    }

    @Override
    protected AttrListProvider dataProvider() {
        return new AccessPolicyRequiredAttrsProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_ACCESS_POLICY_CONF_REQUIRED_ATTRS_PAGINATOR_ROWS;
    }

    protected final class AccessPolicyRequiredAttrsProvider extends AttrListProvider {

        private static final long serialVersionUID = -185944053385660794L;

        private AccessPolicyRequiredAttrsProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        protected List<Attr> list() {
            return model.getObject().getConf().getRequiredAttrs();
        }
    }
}
