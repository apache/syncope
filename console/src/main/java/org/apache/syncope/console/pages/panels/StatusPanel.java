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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.syncope.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.console.commons.status.Status;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.pages.ConnObjectModalPage;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusPanel extends Panel implements IHeaderContributor {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StatusPanel.class);

    private static final long serialVersionUID = -4064294905566247728L;

    public static final String IMG_STATUES = "../statuses/";

    private static final int CONNOBJECT_WIN_HEIGHT = 400;

    private static final int CONNOBJECT_WIN_WIDTH = 600;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final ModalWindow connObjectWin;

    private final List<ConnObjectWrapper> connObjects;

    private final Map<String, StatusBean> initialStatusBeanMap;

    private final CheckGroup<StatusBean> checkGroup;

    private final ListView<StatusBean> statusBeansListView;

    private final StatusUtils statusUtils;

    public <T extends AbstractAttributableTO> StatusPanel(
            final String id,
            final AbstractAttributableTO attributable,
            final List<StatusBean> selectedResources,
            final PageReference pageref) {

        super(id);

        connObjectWin = new ModalWindow("connObjectWin");
        connObjectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        connObjectWin.setInitialHeight(CONNOBJECT_WIN_HEIGHT);
        connObjectWin.setInitialWidth(CONNOBJECT_WIN_WIDTH);
        connObjectWin.setCookieName("connobject-modal");
        add(connObjectWin);

        statusUtils = new StatusUtils(attributable instanceof RoleTO ? roleRestClient : userRestClient);

        connObjects = statusUtils.getConnectorObjects(attributable);

        final List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size() + 1);
        initialStatusBeanMap = new LinkedHashMap<String, StatusBean>(connObjects.size() + 1);

        final StatusBean syncope = new StatusBean(attributable, "syncope");

        if (attributable instanceof UserTO) {
            syncope.setAccountLink(((UserTO) attributable).getUsername());

            Status syncopeStatus = Status.UNDEFINED;
            if (((UserTO) attributable).getStatus() != null) {
                try {
                    syncopeStatus = Status.valueOf(((UserTO) attributable).getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unexpected status found: {}", ((UserTO) attributable).getStatus(), e);
                }
            }
            syncope.setStatus(syncopeStatus);
        } else if (attributable instanceof RoleTO) {
            syncope.setAccountLink(((RoleTO) attributable).getDisplayName());
            syncope.setStatus(Status.ACTIVE);
        }

        statusBeans.add(syncope);
        initialStatusBeanMap.put(syncope.getResourceName(), syncope);

        for (ConnObjectWrapper entry : connObjects) {
            final StatusBean statusBean = statusUtils.getStatusBean(
                    entry.getAttributable(),
                    entry.getResourceName(),
                    entry.getConnObjectTO(),
                    attributable instanceof RoleTO);

            initialStatusBeanMap.put(entry.getResourceName(), statusBean);
            statusBeans.add(statusBean);
        }

        checkGroup = new CheckGroup<StatusBean>("group", selectedResources);
        checkGroup.setOutputMarkupId(true);
        checkGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // ignore
            }
        });
        add(checkGroup);

        CheckGroupSelector groupSelector = new CheckGroupSelector("groupselector", checkGroup);
        if (attributable instanceof RoleTO) {
            groupSelector.setVisible(false);
        }
        add(groupSelector);

        statusBeansListView = new AltListView<StatusBean>("resources", statusBeans) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<StatusBean> item) {
                item.add(statusUtils.getStatusImage("icon", item.getModelObject().getStatus()));

                final Check<StatusBean> check = new Check<StatusBean>("check", item.getModel(), checkGroup);
                if (attributable instanceof RoleTO) {
                    check.setVisible(false);
                }
                item.add(check);

                item.add(new Label("resource", new ResourceModel(item.getModelObject().getResourceName(), item
                        .getModelObject().getResourceName())));

                if (StringUtils.isNotBlank(item.getModelObject().getAccountLink())) {
                    item.add(new Label("accountLink", new ResourceModel(item.getModelObject().getAccountLink(),
                            item.getModelObject().getAccountLink())));
                } else {
                    item.add(new Label("accountLink", ""));
                }

                final ConnObjectTO connObjectTO = statusUtils.getConnObjectTO(
                        item.getModelObject().getAttributableId(),
                        item.getModelObject().getResourceName(),
                        connObjects);

                if (pageref == null || connObjectTO == null) {
                    item.add(new Label("connObject", new Model<String>()));
                } else {
                    final ActionLinksPanel connObject = new ActionLinksPanel("connObject", new Model(), pageref);

                    connObject.add(new ActionLink() {

                        private static final long serialVersionUID = -3722207913631435501L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            connObjectWin.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = -7834632442532690940L;

                                @Override
                                public Page createPage() {
                                    return new ConnObjectModalPage(connObjectTO);
                                }
                            });

                            connObjectWin.show(target);
                        }
                    }, ActionLink.ActionType.SEARCH, "Resources", "getConnectorObject");

                    item.add(connObject);
                }
            }
        };
        statusBeansListView.setReuseItems(true);
        checkGroup.add(statusBeansListView);
    }

    public StatusMod getStatusMod() {
        StatusMod result = null;

        Collection<StatusBean> statusBeans = checkGroup.getModel().getObject();
        if (statusBeans != null && !statusBeans.isEmpty()) {
            result = StatusUtils.buildStatusMod(statusBeans);

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
                    && statusBean.getStatus() == Status.NOT_YET_SUBMITTED) {

                checkGroup.getModelObject().add(statusBean);
            }
        }
    }
}
