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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.to.ResourceTO;
import org.apache.syncope.to.SyncTaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class SyncTaskModalPage extends AbstractSchedTaskModalPage {

    private static final long serialVersionUID = 2148403203517274669L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    public SyncTaskModalPage(final ModalWindow window, final SyncTaskTO taskTO, final PageReference callerPageRef) {

        super(window, taskTO, callerPageRef);

        final IModel<List<String>> allResources = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                final List<String> resourceNames = new ArrayList<String>();

                for (ResourceTO resourceTO : resourceRestClient.getAllResources()) {

                    resourceNames.add(resourceTO.getName());
                }
                return resourceNames;
            }
        };

        final AjaxDropDownChoicePanel<String> resource = new AjaxDropDownChoicePanel<String>("resource",
                getString("resourceName"), new PropertyModel(taskTO, "resource"));
        resource.setChoices(allResources.getObject());
        resource.setChoiceRenderer(new SelectChoiceRenderer());
        resource.addRequiredLabel();
        resource.setEnabled(taskTO.getId() == 0);
        resource.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");

        profile.add(resource);

        final AjaxDropDownChoicePanel<String> actionsClassName = new AjaxDropDownChoicePanel<String>(
                "actionsClassName", getString("actionsClass"), new PropertyModel(taskTO, "actionsClassName"));
        actionsClassName.setChoices(taskRestClient.getSyncActionsClasses());
        actionsClassName.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
        profile.add(actionsClassName);

        final AjaxCheckBoxPanel creates = new AjaxCheckBoxPanel("performCreate", getString("creates"),
                new PropertyModel<Boolean>(taskTO, "performCreate"));
        profile.add(creates);

        final AjaxCheckBoxPanel updates = new AjaxCheckBoxPanel("performUpdate", getString("updates"),
                new PropertyModel<Boolean>(taskTO, "performUpdate"));
        profile.add(updates);

        final AjaxCheckBoxPanel deletes = new AjaxCheckBoxPanel("performDelete", getString("updates"),
                new PropertyModel<Boolean>(taskTO, "performDelete"));
        profile.add(deletes);

        final AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel("syncStatus", getString("syncStatus"),
                new PropertyModel<Boolean>(taskTO, "syncStatus"));
        profile.add(syncStatus);

        final AjaxCheckBoxPanel fullReconciliation = new AjaxCheckBoxPanel("fullReconciliation",
                getString("fullReconciliation"), new PropertyModel<Boolean>(taskTO, "fullReconciliation"));
        profile.add(fullReconciliation);
    }
}
