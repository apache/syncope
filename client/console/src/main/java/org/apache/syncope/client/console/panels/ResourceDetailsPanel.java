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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.any.AnnotatedBeanPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class ResourceDetailsPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    private final IModel<List<String>> propagationActionsClasses = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getPropagationActions());
        }
    };

    public ResourceDetailsPanel(
            final String id,
            final IModel<ResourceTO> model,
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

        container.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                "propagationPriority",
                "propagationPriority",
                Integer.class,
                new PropertyModel<Integer>(model, "propagationPriority")));

        container.add(new AjaxCheckBoxPanel("randomPwdIfNotProvided",
                new ResourceModel("randomPwdIfNotProvided", "randomPwdIfNotProvided").getObject(),
                new PropertyModel<Boolean>(model, "randomPwdIfNotProvided"),
                false));

        container.add(new AjaxPalettePanel.Builder<String>().
                setAllowMoveAll(true).setAllowOrder(true).
                build("propagationActionsClassNames",
                        new PropertyModel<List<String>>(model, "propagationActionsClassNames"),
                        new ListModel<>(propagationActionsClasses.getObject())).
                setOutputMarkupId(true));

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
                "pullTraceLevel",
                new ResourceModel("pullTraceLevel", "pullTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(model, "pullTraceLevel"),
                false).
                setChoices(Arrays.asList(TraceLevel.values())));

        container.add(new AjaxTextFieldPanel(
                "connector",
                new ResourceModel("connector", "connector").getObject(),
                new Model<>(model.getObject().getConnectorDisplayName()),
                false).addRequiredLabel().setEnabled(false));

        add(new AnnotatedBeanPanel("systeminformation", model.getObject()).setRenderBodyOnly(true));
    }
}
