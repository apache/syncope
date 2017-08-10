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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.policies.PolicyRuleDirectoryPanel;
import org.apache.syncope.client.console.reports.ReportletDirectoryPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.console.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toggle panel.
 *
 * @param <T> model object type
 */
public abstract class TogglePanel<T extends Serializable> extends WizardMgtPanel<T> {

    private static final long serialVersionUID = -2025535531121434056L;

    protected static final Logger LOG = LoggerFactory.getLogger(TogglePanel.class);

    private enum Status {
        INACTIVE,
        ACTIVE

    }

    private final WebMarkupContainer container;

    private Status status = Status.INACTIVE;

    private final Label header;

    private final String activeId;

    public TogglePanel(final String id, final PageReference pageRef) {
        this(id, id, pageRef);
    }

    public TogglePanel(final String id, final String markupId, final PageReference pageRef) {
        super(id, true);
        this.activeId = markupId;
        setRenderBodyOnly(true);
        setOutputMarkupId(true);
        disableContainerAutoRefresh();
        setPageRef(pageRef);

        container = new WebMarkupContainer("togglePanelContainer");
        super.addInnerObject(container.setMarkupId(markupId == null ? id : markupId));

        header = new Label("label", StringUtils.EMPTY);
        header.setOutputMarkupId(true);
        container.add(header);

        container.add(new AjaxLink<Void>("close") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                toggle(target, false);
            }

        }.add(new AjaxEventBehavior(Constants.ON_CLICK) {

            private static final long serialVersionUID = -9027652037484739586L;

            @Override
            protected String findIndicatorId() {
                return StringUtils.EMPTY;
            }

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                // do nothing
            }
        }));
    }

    /**
     * Add object inside the main container.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    @Override
    public TogglePanel<T> addInnerObject(final Component... childs) {
        container.addOrReplace(childs);
        return this;
    }

    protected void setHeader(final AjaxRequestTarget target, final String header) {
        this.header.setDefaultModelObject(header == null
                ? StringUtils.EMPTY
                : header.length() >= 40 ? (header.substring(0, 30) + " ... ") : header);
        target.add(this.header);
    }

    public void close(final AjaxRequestTarget target) {
        toggle(target, false);
    }

    /**
     * Force toggle via java. To be used when the onclick has been intercepted before.
     *
     * @param target ajax request target.
     * @param toggle toggle action.
     */
    public void toggle(final AjaxRequestTarget target, final boolean toggle) {
        final String selector = String.format("$(\"div#%s\")", activeId);
        if (toggle) {
            if (status == Status.INACTIVE) {
                target.add(TogglePanel.this.container);
                target.appendJavaScript(
                        selector + ".toggle(\"slow\");"
                        + selector + ".attr(\"class\", \"toggle-menu active-toggle-menu\");");
                status = Status.ACTIVE;
            } else if (status == Status.ACTIVE) {
                // useful when handling action menu after refreshing (ref. SYNCOPE-1134)
                target.appendJavaScript(
                        selector + ".not(':visible')" + ".toggle(\"slow\")" + ".removeClass(\"inactive-toggle-menu\")"
                        + ".addClass(\"active-toggle-menu\");");
            }
        } else if (status == Status.ACTIVE) {
            target.appendJavaScript(
                    selector + ".toggle(\"slow\");"
                    + selector + ".attr(\"class\", \"toggle-menu inactive-toggle-menu\");");
            status = Status.INACTIVE;
        }
    }

    protected String getHeader(final T modelObject) {
        final String headerValue;
        if (modelObject == null) {
            headerValue = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        } else if (modelObject instanceof UserTO) {
            headerValue = ((UserTO) modelObject).getUsername();
        } else if (modelObject instanceof GroupTO) {
            headerValue = ((GroupTO) modelObject).getName();
        } else if (modelObject instanceof UserWrapper) {
            headerValue = UserWrapper.class.cast(modelObject).getInnerObject().getUsername();
        } else if (modelObject instanceof GroupWrapper) {
            headerValue = GroupWrapper.class.cast(modelObject).getInnerObject().getName();
        } else if (modelObject instanceof AnyWrapper) {
            AnyTO anyWrapper = AnyWrapper.class.cast(modelObject).getInnerObject();
            if (anyWrapper instanceof AnyObjectTO) {
                headerValue = ((AnyObjectTO) anyWrapper).getName();
            } else {
                headerValue = new ResourceModel("actions", StringUtils.EMPTY).getObject();
            }
        } else if (modelObject instanceof ConnInstanceTO) {
            headerValue = ConnInstanceTO.class.cast(modelObject).getDisplayName();
        } else if (modelObject instanceof ReportTO) {
            headerValue = ((ReportTO) modelObject).getName();
        } else if (modelObject instanceof AnyObjectTO) {
            headerValue = ((AnyObjectTO) modelObject).getName();
        } else if (modelObject instanceof AttrTO) {
            headerValue = ((AttrTO) modelObject).getSchema();
        } else if (modelObject instanceof AbstractPolicyTO) {
            headerValue = ((AbstractPolicyTO) modelObject).getDescription();
        } else if (modelObject instanceof SecurityQuestionTO) {
            headerValue = ((SecurityQuestionTO) modelObject).getContent();
        } else if (modelObject instanceof AccessTokenTO) {
            headerValue = ((AccessTokenTO) modelObject).getOwner();
        } else if (modelObject instanceof ExecTO) {
            headerValue = ((ExecTO) modelObject).getKey();
        } else if (modelObject instanceof WorkflowDefinitionTO) {
            headerValue = ((WorkflowDefinitionTO) modelObject).getName();
        } else if (modelObject instanceof SchedTaskTO) {
            headerValue = ((SchedTaskTO) modelObject).getName();
        } else if (modelObject instanceof WorkflowFormTO) {
            headerValue = ((WorkflowFormTO) modelObject).getKey();
        } else if (modelObject instanceof EntityTO) {
            headerValue = ((EntityTO) modelObject).getKey();
        } else if (modelObject instanceof StatusBean) {
            headerValue = ((StatusBean) modelObject).getResource();
        } else if (modelObject instanceof PolicyRuleDirectoryPanel.PolicyRuleWrapper) {
            headerValue = ((PolicyRuleDirectoryPanel.PolicyRuleWrapper) modelObject).getName();
        } else if (modelObject instanceof PolicyRuleDirectoryPanel.PolicyRuleWrapper) {
            headerValue = ((PolicyRuleDirectoryPanel.PolicyRuleWrapper) modelObject).getName();
        } else if (modelObject instanceof ReportletDirectoryPanel.ReportletWrapper) {
            headerValue = ((ReportletDirectoryPanel.ReportletWrapper) modelObject).getName();
        } else if (modelObject instanceof JobTO) {
            headerValue = ((JobTO) modelObject).getRefKey() == null
                    ? ((JobTO) modelObject).getRefDesc() : ((JobTO) modelObject).getRefKey();
        } else {
            headerValue = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        }

        return headerValue;
    }

    public static class ActionLinkToggleUpdateEventPayload<T> {

        private final AjaxRequestTarget target;

        private final T modelObj;

        public ActionLinkToggleUpdateEventPayload(final AjaxRequestTarget target, final T modelObj) {
            this.target = target;
            this.modelObj = modelObj;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public T getModelObj() {
            return modelObj;
        }

    }
}
