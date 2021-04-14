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
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.policies.PolicyRuleWrapper;
import org.apache.syncope.client.console.reports.ReportletWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;
import org.apache.syncope.client.console.panels.ToggleableTarget;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.to.NamedEntityTO;

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
        if (modelObject == null) {
            header = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        } else if (modelObject instanceof UserTO) {
            header = ((UserTO) modelObject).getUsername();
        } else if (modelObject instanceof UserWrapper) {
            header = ((UserWrapper) modelObject).getInnerObject().getUsername();
        } else if (modelObject instanceof GroupTO) {
            header = ((GroupTO) modelObject).getName();
        } else if (modelObject instanceof GroupWrapper) {
            header = ((GroupWrapper) modelObject).getInnerObject().getName();
        } else if (modelObject instanceof AnyObjectTO) {
            header = ((AnyObjectTO) modelObject).getName();
        } else if (modelObject instanceof AnyWrapper
                && AnyWrapper.class.cast(modelObject).getInnerObject() instanceof AnyObjectTO) {
            header = ((AnyObjectTO) ((AnyWrapper) modelObject).getInnerObject()).getName();
        } else if (modelObject instanceof Attr) {
            header = ((Attr) modelObject).getSchema();
        } else if (modelObject instanceof ConfParam) {
            header = ((ConfParam) modelObject).getSchema();
        } else if (modelObject instanceof PolicyTO) {
            header = ((PolicyTO) modelObject).getName();
        } else if (modelObject instanceof SecurityQuestionTO) {
            header = ((SecurityQuestionTO) modelObject).getContent();
        } else if (modelObject instanceof AccessTokenTO) {
            header = ((AccessTokenTO) modelObject).getOwner();
        } else if (modelObject instanceof ExecTO) {
            header = ((ExecTO) modelObject).getKey();
        } else if (modelObject instanceof StatusBean) {
            header = ((StatusBean) modelObject).getResource();
        } else if (modelObject instanceof PolicyRuleWrapper) {
            header = ((PolicyRuleWrapper) modelObject).getImplementationKey();
        } else if (modelObject instanceof ReportletWrapper) {
            header = ((ReportletWrapper) modelObject).getImplementationKey();
        } else if (modelObject instanceof JobTO) {
            header = ((JobTO) modelObject).getRefKey() == null
                    ? ((JobTO) modelObject).getRefDesc() : ((JobTO) modelObject).getRefKey();
        } else if (modelObject instanceof ToggleableTarget) {
            header = ((ToggleableTarget) modelObject).getAnyType();
        } else if (modelObject instanceof Domain) {
            header = ((Domain) modelObject).getKey();
        } else if (modelObject instanceof NamedEntityTO) {
            header = ((NamedEntityTO) modelObject).getName();
        } else if (modelObject instanceof EntityTO) {
            header = ((EntityTO) modelObject).getKey();
        } else {
            header = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        }

        setHeader(target, StringUtils.abbreviate(header, HEADER_FIRST_ABBREVIATION));
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
