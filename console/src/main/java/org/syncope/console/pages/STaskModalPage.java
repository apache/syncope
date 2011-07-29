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
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
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

/**
 * Modal window with Task form (to stop and start execution).
 */
public class STaskModalPage extends SchedTaskModalPage {

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

        final DropDownChoice<ResourceTO> resource =
                new DropDownChoice(
                "resource",
                new PropertyModel(taskTO, "resource"),
                allResources,
                new SelectChoiceRenderer());

        profile.add(resource);

        final CheckBox updates = new CheckBox("updateIdentities",
                new PropertyModel<Boolean>(taskTO, "updateIdentities"));
        profile.add(updates);

        final Palette<String> defaultResources = new Palette(
                "defaultResources",
                new PropertyModel(taskTO, "defaultResources"),
                new ListModel<String>(allResources.getObject()),
                new SelectChoiceRenderer(), 8, false);

        form.add(defaultResources);

        final Palette<Long> defaultRoles = new Palette(
                "defaultRoles",
                new PropertyModel(taskTO, "defaultRoles"),
                new ListModel<Long>(
                new ArrayList<Long>(allRoles.getObject().keySet())),
                new ChoiceRenderer<Long>() {

                    @Override
                    public String getDisplayValue(Long id) {
                        return allRoles.getObject().get(id);
                    }

                    @Override
                    public String getIdValue(Long id, int index) {
                        return id.toString();
                    }
                }, 8, false);

        form.add(defaultRoles);
    }
}
