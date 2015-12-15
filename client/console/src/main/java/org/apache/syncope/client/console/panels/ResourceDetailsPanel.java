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

import org.apache.syncope.client.console.wizards.any.AnnotatedBeanPanel;
import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceDetailsPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceDetailsPanel.class);

    public ResourceDetailsPanel(
            final String id,
            final IModel<ResourceTO> model,
            final List<String> actionClassNames,
            final boolean createFlag) {

        super(id);
        setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(true);
        add(container);

        container.add(new AjaxTextFieldPanel(
                "name",
                new ResourceModel("name", "name").
                getObject(),
                new PropertyModel<String>(model, "key"),
                false).addRequiredLabel().setEnabled(createFlag));

        container.add(new AjaxCheckBoxPanel(
                "enforceMandatoryCondition",
                new ResourceModel("enforceMandatoryCondition", "enforceMandatoryCondition").getObject(),
                new PropertyModel<Boolean>(model, "enforceMandatoryCondition"),
                false));

        container.add(new SpinnerFieldPanel<>(
                "propagationPriority",
                "propagationPriority",
                Integer.class,
                new PropertyModel<Integer>(model, "propagationPriority")));

        container.add(new AjaxCheckBoxPanel("randomPwdIfNotProvided",
                new ResourceModel("randomPwdIfNotProvided", "randomPwdIfNotProvided").getObject(),
                new PropertyModel<Boolean>(model, "randomPwdIfNotProvided"),
                false));

        container.add(new MultiFieldPanel.Builder<>(
                new PropertyModel<List<String>>(model, "propagationActionsClassNames")).build(
                        "actionsClasses",
                        "actionsClasses",
                        new AjaxDropDownChoicePanel<>("panel", "panel", new Model<String>())
                        .setChoices(actionClassNames).setNullValid(true).setRequired(true)));

        container.add(new AjaxDropDownChoicePanel<>(
                "createTraceLevel",
                new ResourceModel("createTraceLevel", "createTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(model, "createTraceLevel"),
                false).
                setChoices(Arrays.asList(TraceLevel.values())));

        container.add(new AjaxDropDownChoicePanel<>(
                "updateTraceLevel",
                new ResourceModel("updateTraceLevel", "updateTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(model, "updateTraceLevel"),
                false).
                setChoices(Arrays.asList(TraceLevel.values())));

        container.add(new AjaxDropDownChoicePanel<>(
                "deleteTraceLevel",
                new ResourceModel("deleteTraceLevel", "deleteTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(model, "deleteTraceLevel"),
                false).
                setChoices(Arrays.asList(TraceLevel.values())));

        container.add(new AjaxDropDownChoicePanel<>(
                "syncTraceLevel",
                new ResourceModel("syncTraceLevel", "syncTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(model, "syncTraceLevel"),
                false).
                setChoices(Arrays.asList(TraceLevel.values())));

        container.add(new AjaxTextFieldPanel(
                "connector",
                new ResourceModel("connector", "connector").getObject(),
                new Model<String>(model.getObject().getConnectorDisplayName()),
                false).addRequiredLabel().setEnabled(false));

        add(new AnnotatedBeanPanel("systeminformation", model.getObject()).setRenderBodyOnly(true));
    }
}
