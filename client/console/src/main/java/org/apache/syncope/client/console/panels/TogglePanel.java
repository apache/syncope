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
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWrapper;
import org.apache.syncope.client.console.wizards.any.UserWrapper;
import org.apache.syncope.client.console.wizards.resources.ResourceProvision;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
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
import org.apache.wicket.AttributeModifier;
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
    
    protected static final int HEADER_FIRST_ABBREVIATION = 25;

    private enum Status {
        INACTIVE,
        ACTIVE

    }

    private static final String LABEL_DATA_VALUE = "data-value";

    private enum ToggleMenuCSS {
        CLASS("toggle-menu"),
        CLASS_ACTIVE("active-toggle-menu"),
        CLASS_INACTIVE("active-toggle-menu");

        private final String value;

        ToggleMenuCSS(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

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
        final String containerID = StringUtils.isBlank(markupId) ? id : markupId;

        setRenderBodyOnly(true);
        setOutputMarkupId(true);
        disableContainerAutoRefresh();
        setPageRef(pageRef);

        container = new WebMarkupContainer("togglePanelContainer");
        super.addInnerObject(container.setMarkupId(containerID));

        header = new Label("label", StringUtils.EMPTY);
        header.add(new AttributeModifier("title", new ResourceModel("copy_to_clipboard.title")));
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

    @SuppressWarnings("cast")
    private String getTargetKey(final Serializable modelObject) {
        final String key;
        if (modelObject == null) {
            key = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        } else if (modelObject instanceof UserTO) {
            key = ((UserTO) modelObject).getKey();
        } else if (modelObject instanceof UserWrapper) {
            key = ((UserWrapper) modelObject).getInnerObject().getKey();
        } else if (modelObject instanceof GroupTO) {
            key = ((GroupTO) modelObject).getKey();
        } else if (modelObject instanceof GroupWrapper) {
            key = ((GroupWrapper) modelObject).getInnerObject().getKey();
        } else if (modelObject instanceof AnyObjectTO) {
            key = ((AnyObjectTO) modelObject).getKey();
        } else if (modelObject instanceof AnyWrapper
                && AnyWrapper.class.cast(modelObject).getInnerObject() instanceof AnyObjectTO) {
            key = ((AnyObjectTO) ((AnyWrapper) modelObject).getInnerObject()).getKey();
        } else if (modelObject instanceof ReportTO) {
            key = ((ReportTO) modelObject).getKey();
        } else if (modelObject instanceof AttrTO) {
            key = ((AttrTO) modelObject).getSchemaInfo().getKey();
        } else if (modelObject instanceof AbstractPolicyTO) {
            key = ((AbstractPolicyTO) modelObject).getKey();
        } else if (modelObject instanceof SecurityQuestionTO) {
            key = ((SecurityQuestionTO) modelObject).getKey();
        } else if (modelObject instanceof AccessTokenTO) {
            key = ((AccessTokenTO) modelObject).getKey();
        } else if (modelObject instanceof ExecTO) {
            key = ((ExecTO) modelObject).getKey();
        } else if (modelObject instanceof WorkflowDefinitionTO) {
            key = ((WorkflowDefinitionTO) modelObject).getKey();
        } else if (modelObject instanceof SchedTaskTO) {
            key = ((SchedTaskTO) modelObject).getKey();
        } else if (modelObject instanceof WorkflowFormTO) {
            key = ((WorkflowFormTO) modelObject).getKey();
        } else if (modelObject instanceof EntityTO) {
            key = ((EntityTO) modelObject).getKey();
        } else if (modelObject instanceof StatusBean) {
            key = ((StatusBean) modelObject).getKey();
        } else if (modelObject instanceof PolicyRuleDirectoryPanel.PolicyRuleWrapper) {
            key = ((PolicyRuleDirectoryPanel.PolicyRuleWrapper) modelObject).getName();
        } else if (modelObject instanceof PolicyRuleDirectoryPanel.PolicyRuleWrapper) {
            key = ((PolicyRuleDirectoryPanel.PolicyRuleWrapper) modelObject).getName();
        } else if (modelObject instanceof ReportletDirectoryPanel.ReportletWrapper) {
            key = ((ReportletDirectoryPanel.ReportletWrapper) modelObject).getName();
        } else if (modelObject instanceof JobTO) {
            key = ((JobTO) modelObject).getRefKey() == null
                    ? ((JobTO) modelObject).getRefDesc() : ((JobTO) modelObject).getRefKey();
        } else if (modelObject instanceof ResourceProvision) {
            key = ((ResourceProvision) modelObject).getKey();
        } else if (modelObject instanceof TopologyNode) {
            key = ((TopologyNode) modelObject).getKey();
        } else {
            key = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        }
        return key;
    }

    private void updateLabelKeyValue(final Serializable modelObject) {
        header.add(new AttributeModifier(LABEL_DATA_VALUE, getTargetKey(modelObject)));
    }

    /**
     * Force toggle via java. To be used when the onclick has been intercepted before.
     * Also, set key value in label name for copy-to-clipboard feature.
     *
     * @param target ajax request target.
     * @param modelObject model object
     * @param toggle toggle action.
     */
    public void toggle(final AjaxRequestTarget target, final Serializable modelObject, final boolean toggle) {
        updateLabelKeyValue(modelObject);

        toggle(target, toggle);
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
                        + selector + ".attr(\"class\", \""
                        + ToggleMenuCSS.CLASS.value() + " " + ToggleMenuCSS.CLASS_ACTIVE.value() + "\");");
                status = Status.ACTIVE;
            } else if (status == Status.ACTIVE) {
                // useful when handling action menu after refreshing (ref. SYNCOPE-1134)
                target.appendJavaScript(
                        selector + ".not(':visible')" + ".toggle(\"slow\")" + ".removeClass(\""
                        + ToggleMenuCSS.CLASS_INACTIVE.value() + "\")"
                        + ".addClass(\"" + ToggleMenuCSS.CLASS_ACTIVE.value() + "y\");");
            }
        } else if (status == Status.ACTIVE) {
            target.appendJavaScript(
                    selector + ".toggle(\"slow\");"
                    + selector + ".attr(\"class\", \""
                    + ToggleMenuCSS.CLASS.value() + " " + ToggleMenuCSS.CLASS_INACTIVE.value() + "\");");
            status = Status.INACTIVE;
        }
    }
}
