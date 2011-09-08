/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxPalettePanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class STaskModalPage extends SchedTaskModalPage {

    private static final long serialVersionUID = 2148403203517274669L;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public STaskModalPage(
            final BasePage basePage,
            final ModalWindow window,
            final SyncTaskTO taskTO) {

        super(basePage, window, taskTO);

        final IModel<List<String>> allResources =
                new LoadableDetachableModel<List<String>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<String> load() {
                        final List<String> resourceNames =
                                new ArrayList<String>();

                        for (ResourceTO resourceTO :
                                resourceRestClient.getAllResources()) {
                            resourceNames.add(resourceTO.getName());
                        }
                        return resourceNames;
                    }
                };

        final IModel<Map<Long, String>> allRoles =
                new LoadableDetachableModel<Map<Long, String>>() {

                    private static final long serialVersionUID =
                            -2012833443695917883L;

                    @Override
                    protected Map<Long, String> load() {
                        final Map<Long, String> allRoles =
                                new HashMap<Long, String>();

                        List<RoleTO> roles = roleRestClient.getAllRoles();

                        if (roles != null) {
                            for (RoleTO role : roles) {
                                allRoles.put(role.getId(), role.getName());
                            }
                        }

                        return allRoles;
                    }
                };

        final AjaxDropDownChoicePanel<String> resource =
                new AjaxDropDownChoicePanel<String>(
                "resource", getString("resourceName"),
                new PropertyModel(taskTO, "resource"), false);
        resource.setChoices(allResources.getObject());
        resource.setChoiceRenderer(new SelectChoiceRenderer());
        resource.addRequiredLabel();
        resource.setEnabled(taskTO.getId() == 0);
        resource.setStyleShet(
                "ui-widget-content ui-corner-all long_dynamicsize");

        profile.add(resource);

        final AjaxCheckBoxPanel updates = new AjaxCheckBoxPanel(
                "updateIdentities", getString("updates"),
                new PropertyModel<Boolean>(taskTO, "updateIdentities"), false);

        profile.add(updates);

        final AjaxPalettePanel defaultResources = new AjaxPalettePanel(
                "defaultResources",
                new PropertyModel(taskTO, "defaultResources"),
                new ListModel<String>(allResources.getObject()));

        form.add(defaultResources);

        final AjaxPalettePanel<Long> defaultRoles = new AjaxPalettePanel<Long>(
                "defaultRoles",
                new PropertyModel(taskTO, "defaultRoles"), new ListModel<Long>(
                new ArrayList<Long>(allRoles.getObject().keySet())),
                new ChoiceRenderer<Long>() {

                    private static final long serialVersionUID =
                            8463000788871139550L;

                    @Override
                    public String getDisplayValue(Long id) {
                        return allRoles.getObject().get(id);
                    }

                    @Override
                    public String getIdValue(Long id, int index) {
                        return id.toString();
                    }
                });

        form.add(defaultRoles);
    }
}
