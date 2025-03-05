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
package org.apache.syncope.client.console.wicket.markup.html.form;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.ConfParam;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.panels.ToggleableTarget;
import org.apache.syncope.client.console.policies.PolicyRuleWrapper;
import org.apache.syncope.client.console.tasks.CommandWrapper;
import org.apache.syncope.client.console.wizards.any.AnyObjectWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NamedEntityTO;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;

public class ActionLinksTogglePanel<T extends Serializable> extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -2025535531121434056L;

    private final WebMarkupContainer container;

    public ActionLinksTogglePanel(final String id, final PageReference pageRef) {
        super(id, UUID.randomUUID().toString(), pageRef);

        modal.size(Modal.Size.Large);
        setFooterVisibility(false);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true);
        addInnerObject(container);

        container.add(getEmptyFragment());
    }

    public void updateHeader(final AjaxRequestTarget target, final Serializable modelObject) {
        final String header;
        if (modelObject instanceof final UserTO userTO) {
            header = userTO.getUsername();
        } else if (modelObject instanceof final UserWrapper userWrapper) {
            header = userWrapper.getInnerObject().getUsername();
        } else if (modelObject instanceof final GroupTO groupTO) {
            header = groupTO.getName();
        } else if (modelObject instanceof final GroupWrapper groupWrapper) {
            header = groupWrapper.getInnerObject().getName();
        } else if (modelObject instanceof final AnyObjectTO anyObjectTO) {
            header = anyObjectTO.getName();
        } else if (modelObject instanceof final AnyObjectWrapper anyObjectWrapper) {
            header = anyObjectWrapper.getInnerObject().getName();
        } else if (modelObject instanceof final Attr attr) {
            header = attr.getSchema();
        } else if (modelObject instanceof final ConfParam confParam) {
            header = confParam.getSchema();
        } else if (modelObject instanceof final PolicyTO policyTO) {
            header = policyTO.getName();
        } else if (modelObject instanceof final SecurityQuestionTO securityQuestionTO) {
            header = securityQuestionTO.getContent();
        } else if (modelObject instanceof final AccessTokenTO accessTokenTO) {
            header = accessTokenTO.getOwner();
        } else if (modelObject instanceof final ExecTO execTO) {
            header = execTO.getKey();
        } else if (modelObject instanceof final StatusBean statusBean) {
            header = statusBean.getResource();
        } else if (modelObject instanceof final PolicyRuleWrapper policyRuleWrapper) {
            header = policyRuleWrapper.getImplementationKey();
        } else if (modelObject instanceof final CommandWrapper commandWrapper) {
            header = commandWrapper.getCommand().getKey();
        } else if (modelObject instanceof final JobTO jobTO) {
            header = jobTO.getRefKey() == null
                    ? jobTO.getRefDesc() : jobTO.getRefKey();
        } else if (modelObject instanceof final ToggleableTarget toggleableTarget) {
            header = toggleableTarget.getAnyType();
        } else if (modelObject instanceof final Domain domain) {
            header = domain.getKey();
        } else if (modelObject instanceof final CommandTO commandTO) {
            header = commandTO.getKey();
        } else if (modelObject instanceof final NamedEntityTO entity) {
            header = entity.getName();
        } else if (modelObject instanceof final EntityTO entityTO) {
            header = entityTO.getKey();
        } else if (modelObject instanceof final EntityWrapper entityWrapper) {
            EntityTO inner = entityWrapper.getInnerObject();
            header = inner instanceof final NamedEntityTO namedEntityTO ? namedEntityTO.getName() : inner.getKey();
        } else {
            header = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        }

        setHeader(target, header);
    }

    public void toggleWithContent(
            final AjaxRequestTarget target,
            final ActionsPanel<T> actionsPanel,
            final T modelObject) {

        updateHeader(target, modelObject);

        modal.setWindowClosedCallback(t -> modal.show(false));

        Fragment frag = new Fragment("actions", "actionsFragment", this);
        frag.setOutputMarkupId(true);
        frag.add(actionsPanel);

        container.addOrReplace(frag);
        target.add(this.container);

        toggle(target, modelObject, true);
    }

    private Fragment getEmptyFragment() {
        return new Fragment("actions", "emptyFragment", this);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ActionLinkToggleCloseEventPayload) {
            close(ActionLinkToggleCloseEventPayload.class.cast(event.getPayload()).getTarget());
        }
    }

    public static class ActionLinkToggleCloseEventPayload {

        private final AjaxRequestTarget target;

        public ActionLinkToggleCloseEventPayload(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
