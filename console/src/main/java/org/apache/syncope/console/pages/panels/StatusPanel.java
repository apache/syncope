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
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.commons.StatusUtils;
import org.apache.syncope.console.commons.StatusUtils.Status;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.pages.ConnObjectModalPage;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.image.Image;
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

    private final AbstractAttributableTO attributable;

    private final Map<String, ConnObjectTO> connObjects;

    private final Map<String, StatusBean> initialStatusBeanMap;

    private final CheckGroup<StatusBean> checkGroup;

    private final ListView<StatusBean> statusBeansListView;

    public <T extends AbstractAttributableTO> StatusPanel(final String id, final AbstractAttributableTO attributable,
            final List<StatusBean> selectedResources, final PageReference pageref) {

        super(id);
        this.attributable = attributable;

        connObjectWin = new ModalWindow("connObjectWin");
        connObjectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        connObjectWin.setInitialHeight(CONNOBJECT_WIN_HEIGHT);
        connObjectWin.setInitialWidth(CONNOBJECT_WIN_WIDTH);
        connObjectWin.setCookieName("connobject-modal");
        add(connObjectWin);

        final StatusBean syncope = new StatusBean();
        syncope.setResourceName("Syncope");
        if (attributable instanceof UserTO) {
            UserTO userTO = (UserTO) attributable;
            syncope.setAccountLink(userTO.getUsername());

            Status syncopeStatus = Status.UNDEFINED;
            if (userTO.getStatus() != null) {
                try {
                    syncopeStatus = Status.valueOf(userTO.getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unexpected status found: {}", userTO.getStatus());
                }
            }
            syncope.setStatus(syncopeStatus);
        }
        if (attributable instanceof RoleTO) {
            RoleTO roleTO = (RoleTO) attributable;
            syncope.setAccountLink(roleTO.getDisplayName());
            syncope.setStatus(Status.ACTIVE);
        }

        StatusUtils statusUtils = new StatusUtils((attributable instanceof UserTO ? userRestClient : roleRestClient));

        connObjects = statusUtils.getConnectorObjects(attributable);

        List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size() + 1);
        statusBeans.add(syncope);
        initialStatusBeanMap = new LinkedHashMap<String, StatusBean>(connObjects.size() + 1);
        initialStatusBeanMap.put(syncope.getResourceName(), syncope);
        for (Map.Entry<String, ConnObjectTO> entry : connObjects.entrySet()) {
            final StatusBean statusBean = statusUtils.getStatusBean(entry.getKey(), entry.getValue());

            initialStatusBeanMap.put(entry.getKey(), statusBean);
            statusBeans.add(statusBean);
        }

        checkGroup = new CheckGroup<StatusBean>("group", selectedResources);
        checkGroup.setOutputMarkupId(true);
        add(checkGroup);

        add(new CheckGroupSelector("groupselector", checkGroup));

        statusBeansListView = new AltListView<StatusBean>("resources", statusBeans) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<StatusBean> item) {
                final Image image;
                final String alt, title;
                boolean checkVisibility = true;

                switch (item.getModelObject().getStatus()) {

                    case NOT_YET_SUBMITTED:
                        image = new Image("icon", IMG_STATUES + StatusUtils.Status.UNDEFINED.toString()
                                + SyncopeConstants.DEFAULT_IMG_SUFFIX);
                        alt = "undefined icon";
                        title = "Not yet submitted";
                        break;

                    case ACTIVE:
                        image = new Image("icon", IMG_STATUES + StatusUtils.Status.ACTIVE.toString()
                                + SyncopeConstants.DEFAULT_IMG_SUFFIX);
                        alt = "active icon";
                        title = "Enabled";
                        break;

                    case UNDEFINED:
                        image = new Image("icon", IMG_STATUES + StatusUtils.Status.UNDEFINED.toString()
                                + SyncopeConstants.DEFAULT_IMG_SUFFIX);
                        checkVisibility = false;
                        alt = "undefined icon";
                        title = "Undefined status";
                        break;

                    case OBJECT_NOT_FOUND:
                        image =
                                new Image("icon", IMG_STATUES + StatusUtils.Status.OBJECT_NOT_FOUND.toString()
                                + SyncopeConstants.DEFAULT_IMG_SUFFIX);
                        checkVisibility = false;
                        alt = "notfound icon";
                        title = "User not found";
                        break;

                    default:
                        image = new Image("icon", IMG_STATUES + StatusUtils.Status.SUSPENDED.toString()
                                + SyncopeConstants.DEFAULT_IMG_SUFFIX);
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
                item.add(image);

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

                if (pageref != null
                        && connObjects.containsKey(item.getModelObject().getResourceName())
                        && connObjects.get(item.getModelObject().getResourceName()) != null) {

                    final ConnObjectTO connObjectTO = connObjects.get(item.getModelObject().getResourceName());

                    ActionLinksPanel connObject = new ActionLinksPanel("connObject", new Model(), pageref);

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
                } else {
                    item.add(new Label("connObject", new Model<String>()));
                }
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

    @Override
    public void renderHead(final IHeaderResponse response) {
        if (this.attributable instanceof RoleTO) {
            response.render(new CssContentHeaderItem(
                    "div#check{"
                    + "display:none;"
                    + "}"
                    + "div#status{"
                    + "display:none;"
                    + "}", null, null));
        }
    }
}
