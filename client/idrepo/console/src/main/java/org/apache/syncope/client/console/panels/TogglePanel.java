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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.policies.PolicyRuleWrapper;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.JobTO;
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
        String containerID = StringUtils.isBlank(markupId) ? id : markupId;

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
        this.header.setDefaultModelObject(Optional.ofNullable(header).
                map(s -> StringUtils.abbreviate(s, HEADER_FIRST_ABBREVIATION)).orElse(StringUtils.EMPTY));
        target.add(this.header);
    }

    public void close(final AjaxRequestTarget target) {
        toggle(target, false);
    }

    @SuppressWarnings("cast")
    protected String getTargetKey(final Serializable modelObject) {
        final String key;
        if (modelObject == null) {
            key = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        } else if (modelObject instanceof EntityTO entityTO) {
            key = entityTO.getKey();
        } else if (modelObject instanceof final AnyWrapper<?> anyWrapper) {
            key = anyWrapper.getInnerObject().getKey();
        } else if (modelObject instanceof Attr attr) {
            key = attr.getSchema();
        } else if (modelObject instanceof ConfParam confParam) {
            key = confParam.getSchema();
        } else if (modelObject instanceof StatusBean statusBean) {
            key = StringUtils.isNotBlank(statusBean.getResource())
                    ? statusBean.getResource() : statusBean.getKey();
        } else if (modelObject instanceof PolicyRuleWrapper policyRuleWrapper) {
            key = policyRuleWrapper.getConf().getName();
        } else if (modelObject instanceof JobTO jobTO) {
            key = jobTO.getRefKey() == null ? jobTO.getRefDesc() : jobTO.getRefKey();
        } else if (modelObject instanceof ToggleableTarget toggleableTarget) {
            key = toggleableTarget.getKey();
        } else if (modelObject instanceof Domain domain) {
            key = domain.getKey();
        } else {
            key = new ResourceModel("actions", StringUtils.EMPTY).getObject();
        }
        return key;
    }

    protected void updateLabelKeyValue(final Serializable modelObject) {
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
        String selector = String.format("$(\"div#%s\")", activeId);
        if (toggle) {
            if (status == Status.INACTIVE) {
                target.add(TogglePanel.this.container);
                target.appendJavaScript(
                        selector + ".toggle(\"slow\");"
                        + selector + ".attr(\"class\", \""
                        + ToggleMenuCSS.CLASS.value() + ' ' + ToggleMenuCSS.CLASS_ACTIVE.value() + "\");");
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
                    + ToggleMenuCSS.CLASS.value() + ' ' + ToggleMenuCSS.CLASS_INACTIVE.value() + "\");");
            status = Status.INACTIVE;
        }
    }
}
