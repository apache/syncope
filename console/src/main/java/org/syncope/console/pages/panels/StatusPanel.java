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
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.StatusBean;
import org.syncope.console.commons.StatusUtils;
import org.syncope.console.commons.StatusUtils.Status;

public class StatusPanel extends Panel {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatusPanel.class);

    @SpringBean
    private StatusUtils statusUtils;

    public <T extends AbstractAttributableTO> StatusPanel(
            final String id,
            final UserTO userTO,
            final List<StatusBean> selectedResources) {
        this(id, userTO, selectedResources, true);
    }

    public <T extends AbstractAttributableTO> StatusPanel(
            final String id,
            final UserTO userTO,
            final List<StatusBean> selectedResources,
            final boolean enabled) {

        super(id);

        final List<StatusBean> statuses = new ArrayList<StatusBean>();

        final StatusBean syncope = new StatusBean();
        syncope.setAccountLink(userTO.getUsername());
        syncope.setResourceName("Syncope");
        syncope.setStatus(userTO.getStatus() != null
                ? Status.valueOf(userTO.getStatus().toUpperCase())
                : Status.UNDEFINED);

        statuses.add(syncope);
        statuses.addAll(statusUtils.getRemoteStatuses(userTO));

        final CheckGroup group = new CheckGroup("group", selectedResources);
        add(group);

        final Fragment headerCheckFrag;

        if (enabled) {
            headerCheckFrag = new Fragment(
                    "headerCheck", "headerCheckFrag", this);
            headerCheckFrag.add(
                    new CheckGroupSelector("groupselector", group));
        } else {
            headerCheckFrag = new Fragment(
                    "headerCheck", "emptyCheckFrag", this);
        }

        add(headerCheckFrag);

        final ListView<StatusBean> resources =
                new ListView<StatusBean>("resources", statuses) {

                    private static final long serialVersionUID =
                            4949588177564901031L;

                    @Override
                    protected void populateItem(
                            final ListItem<StatusBean> item) {
                        final Image image;
                        final String alt, title;
                        boolean checkVisibility = true;

                        switch (item.getModelObject().getStatus()) {
                            case ACTIVE:
                                image = new Image(
                                        "icon", "statuses/active.png");
                                alt = "active icon";
                                title = "Enabled";
                                break;
                            case UNDEFINED:
                                image = new Image(
                                        "icon", "statuses/undefined.png");
                                checkVisibility = false;
                                alt = "undefined icon";
                                title = "Undefined status";
                                break;
                            case USER_NOT_FOUND:
                                image = new Image(
                                        "icon", "statuses/usernotfound.png");
                                checkVisibility = false;
                                alt = "notfound icon";
                                title = "User not found";
                                break;
                            default:
                                image = new Image(
                                        "icon", "statuses/inactive.png");
                                alt = "inactive icon";
                                title = "Disabled";
                        }

                        image.add(new Behavior() {

                            private static final long serialVersionUID =
                                    1469628524240283489L;

                            @Override
                            public void onComponentTag(
                                    final Component component,
                                    final ComponentTag tag) {
                                tag.put("alt", alt);
                                tag.put("title", title);
                            }
                        });

                        final Fragment checkFrag;

                        if (!enabled) {
                            checkFrag = new Fragment(
                                    "rowCheck",
                                    "emptyCheckFrag",
                                    group.getParent());
                        } else {
                            final Check check = new Check(
                                    "check", item.getModel(), group);

                            check.setEnabled(checkVisibility);
                            check.setVisible(checkVisibility);

                            checkFrag = new Fragment(
                                    "rowCheck",
                                    "rowCheckFrag",
                                    getParent());

                            checkFrag.add(check);
                        }

                        item.add(checkFrag);

                        item.add(new Label("resource", new ResourceModel(
                                item.getModelObject().getResourceName(),
                                item.getModelObject().getResourceName())));

                        if (StringUtils.isNotBlank(
                                item.getModelObject().getAccountLink())) {

                            item.add(new Label("accountLink", new ResourceModel(
                                    item.getModelObject().getAccountLink(),
                                    item.getModelObject().getAccountLink())));

                        } else {
                            item.add(new Label("accountLink", ""));
                        }

                        item.add(image);
                    }
                };

        resources.setReuseItems(true);

        group.add(resources);
    }
}
