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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.commons.StatusUtils;
import org.apache.syncope.console.commons.StatusUtils.Status;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class StatusPanel extends Panel {

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final StatusUtils statusUtils;

    private final Map<String, StatusBean> initialStatusBeanMap;

    private final CheckGroup<StatusBean> checkGroup;

    private final ListView<StatusBean> statusBeansListView;

    public <T extends AbstractAttributableTO> StatusPanel(final String id, final AbstractAttributableTO attributable,
            final List<StatusBean> selectedResources) {

        super(id);
        statusUtils = new StatusUtils(resourceRestClient,
                (attributable instanceof UserTO ? userRestClient : roleRestClient));

        final List<StatusBean> statusBeans = new ArrayList<StatusBean>();

        final StatusBean syncope = new StatusBean();
        syncope.setResourceName("Syncope");
        if (attributable instanceof UserTO) {
            UserTO userTO = (UserTO) attributable;
            syncope.setAccountLink(userTO.getUsername());
            syncope.setStatus(userTO.getStatus() == null
                    ? Status.UNDEFINED
                    : Status.valueOf(userTO.getStatus().toUpperCase()));
        }
        if (attributable instanceof RoleTO) {
            RoleTO roleTO = (RoleTO) attributable;
            syncope.setAccountLink(roleTO.getDisplayName());
            syncope.setStatus(Status.ACTIVE);
        }

        statusBeans.add(syncope);
        statusBeans.addAll(statusUtils.getRemoteStatuses(attributable));

        initialStatusBeanMap = new HashMap<String, StatusBean>(statusBeans.size());
        for (StatusBean statusBean : statusBeans) {
            initialStatusBeanMap.put(statusBean.getResourceName(), statusBean);
        }

        checkGroup = new CheckGroup<StatusBean>("group", selectedResources) {

            private static final long serialVersionUID = 4085912362037539780L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }
        };
        checkGroup.setOutputMarkupId(true);
        add(checkGroup);

        add(new CheckGroupSelector("groupselector", checkGroup));

        statusBeansListView = new ListView<StatusBean>("resources", statusBeans) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<StatusBean> item) {
                final Image image;
                final String alt, title;
                boolean checkVisibility = true;

                switch (item.getModelObject().getStatus()) {

                    case NOT_YET_SUBMITTED:
                        image = new Image("icon", "../statuses/undefined.png");
                        alt = "undefined icon";
                        title = "Not yet submitted";
                        break;

                    case ACTIVE:
                        image = new Image("icon", "../statuses/active.png");
                        alt = "active icon";
                        title = "Enabled";
                        break;

                    case UNDEFINED:
                        image = new Image("icon", "../statuses/undefined.png");
                        checkVisibility = false;
                        alt = "undefined icon";
                        title = "Undefined status";
                        break;

                    case OBJECT_NOT_FOUND:
                        image = new Image("icon", "../statuses/objectnotfound.png");
                        checkVisibility = false;
                        alt = "notfound icon";
                        title = "User not found";
                        break;

                    default:
                        image = new Image("icon", "../statuses/inactive.png");
                        alt = "inactive icon";
                        title = "Disabled";
                }

                image.add(new Behavior() {

                    private static final long serialVersionUID = 1469628524240283489L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("alt", alt);
                        tag.put("title", title);
                    }
                });

                final Check<StatusBean> check = new Check<StatusBean>("check", item.getModel(), checkGroup);
                check.setEnabled(checkVisibility);
                check.setVisible(checkVisibility);
                item.add(check);

                item.add(new Label("resource", new ResourceModel(item.getModelObject().getResourceName(), item
                        .getModelObject().getResourceName())));

                if (StringUtils.isNotBlank(item.getModelObject().getAccountLink())) {
                    item.add(new Label("accountLink", new ResourceModel(item.getModelObject().getAccountLink(),
                            item.getModelObject().getAccountLink())));
                } else {
                    item.add(new Label("accountLink", ""));
                }

                item.add(image);
            }
        };
        statusBeansListView.setReuseItems(true);
        checkGroup.add(statusBeansListView);
    }

    public PropagationRequestTO getPropagationRequestTO() {
        PropagationRequestTO result = null;

        Collection<StatusBean> statusBeans = checkGroup.getModel().getObject();
        if (statusBeans != null && !statusBeans.isEmpty()) {
            result = StatusUtils.buildPropagationRequestTO(statusBeans);

        }

        return result;
    }

    public List<StatusBean> getStatusBeans() {
        return statusBeansListView.getModelObject();
    }

    public Map<String, StatusBean> getInitialStatusBeanMap() {
        return initialStatusBeanMap;
    }

    public void updateStatusBeans(final List<StatusBean> statusBeans) {
        statusBeansListView.removeAll();
        statusBeansListView.getModelObject().clear();
        statusBeansListView.getModelObject().addAll(statusBeans);

        for (StatusBean statusBean : statusBeans) {
            if (!checkGroup.getModelObject().contains(statusBean)
                    && statusBean.getStatus() == StatusUtils.Status.NOT_YET_SUBMITTED) {

                checkGroup.getModelObject().add(statusBean);
            }
        }
    }
}
