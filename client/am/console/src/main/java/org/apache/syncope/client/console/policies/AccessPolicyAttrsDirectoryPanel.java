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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.panels.AttrListDirectoryPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

public class AccessPolicyAttrsDirectoryPanel extends AttrListDirectoryPanel {

    private static final long serialVersionUID = 33604877627114L;

    private final BaseModal<AccessPolicyTO> wizardModal;

    private final IModel<AccessPolicyTO> accessPolicyModel;

    private final SerializableFunction<AccessPolicyConf, List<Attr>> attrsAccessor;

    public AccessPolicyAttrsDirectoryPanel(
            final String id,
            final BaseModal<AccessPolicyTO> wizardModal,
            final IModel<AccessPolicyTO> model,
            final SerializableFunction<AccessPolicyConf, List<Attr>> attrsAccessor,
            final PageReference pageRef) {

        super(id, pageRef, false);

        this.wizardModal = wizardModal;
        this.accessPolicyModel = model;
        this.attrsAccessor = attrsAccessor;

        setOutputMarkupId(true);

        enableUtilityButton();
        setFooterVisibility(false);

        addNewItemPanelBuilder(
                new AccessPolicyAttrsWizardBuilder(model.getObject(), attrsAccessor, new Attr(), pageRef), true);

        initResultTable();
    }

    @Override
    protected ActionsPanel<Attr> getActions(final IModel<Attr> model) {
        ActionsPanel<Attr> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                try {
                    attrsAccessor.apply(accessPolicyModel.getObject().getConf()).remove(model.getObject());
                    PolicyRestClient.update(PolicyType.ACCESS, accessPolicyModel.getObject());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While updating {}", accessPolicyModel.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.POLICY_UPDATE, true);

        return panel;
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
        return new AccessPolicyAttrsProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_ACCESS_POLICY_CONF_ATTRS_PAGINATOR_ROWS;
    }

    protected final class AccessPolicyAttrsProvider extends AttrListProvider {

        private static final long serialVersionUID = -185944053385660794L;

        private AccessPolicyAttrsProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        protected List<Attr> list() {
            return attrsAccessor.apply(accessPolicyModel.getObject().getConf());
        }
    }
}
