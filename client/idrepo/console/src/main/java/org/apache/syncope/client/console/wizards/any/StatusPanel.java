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
package org.apache.syncope.client.console.wizards.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.panels.PropagationErrorPanel;
import org.apache.syncope.client.console.panels.RemoteObjectPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.ui.commons.status.Status;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusPanel extends Panel {

    private static final long serialVersionUID = -4013796607157549641L;

    protected static final Logger LOG = LoggerFactory.getLogger(StatusPanel.class);

    protected Map<String, StatusBean> initialStatusBeanMap;

    protected ListViewPanel<?> listViewPanel;

    public <T extends AnyTO> StatusPanel(
            final String id,
            final T any,
            final IModel<List<StatusBean>> model,
            final PageReference pageRef) {

        super(id);
        init(any, model,
                SyncopeWebApplication.get().getStatusProvider().get(any, any.getResources()), pageRef, false);
    }

    public <T extends AnyTO> StatusPanel(
            final String id,
            final T any,
            final IModel<List<StatusBean>> model,
            final List<Triple<ConnObject, ConnObjectWrapper, String>> connObjects,
            final PageReference pageRef) {

        super(id);
        init(any, model, connObjects, pageRef, true);
    }

    protected void init(
            final AnyTO any,
            final IModel<List<StatusBean>> model,
            final List<Triple<ConnObject, ConnObjectWrapper, String>> connObjects,
            final PageReference pageRef,
            final boolean enableConnObjectLink) {

        final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size() + 1);
        initialStatusBeanMap = new LinkedHashMap<>(connObjects.size() + 1);

        final StatusBean syncope = new StatusBean(any, Constants.SYNCOPE);

        if (any instanceof final UserTO userTO) {
            syncope.setConnObjectLink(userTO.getUsername());

            Status syncopeStatus = Status.UNDEFINED;
            if (userTO.getStatus() != null) {
                try {
                    syncopeStatus = Status.valueOf(((UserTO) any).getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unexpected status found: {}", ((UserTO) any).getStatus(), e);
                }
            }
            syncope.setStatus(syncopeStatus);
        } else if (any instanceof final GroupTO groupTO) {
            syncope.setConnObjectLink(groupTO.getName());
            syncope.setStatus(Status.ACTIVE);
        }

        statusBeans.add(syncope);
        initialStatusBeanMap.put(syncope.getResource(), syncope);

        Map<String, String> failureReasons = new HashMap<>();
        connObjects.forEach(triple -> {
            ConnObjectWrapper connObjectWrapper = triple.getMiddle();
            StatusBean statusBean = StatusUtils.getStatusBean(connObjectWrapper.getAny(),
                    connObjectWrapper.getResource(),
                    connObjectWrapper.getConnObjectTO(),
                    any instanceof GroupTO);

            initialStatusBeanMap.put(connObjectWrapper.getResource(), statusBean);
            statusBeans.add(statusBean);

            if (StringUtils.isNotBlank(triple.getRight())) {
                failureReasons.put(connObjectWrapper.getResource(), triple.getRight());
            }
        });

        MultilevelPanel mlp = new MultilevelPanel("resources");
        add(mlp);

        ListViewPanel.Builder<StatusBean> builder = new ListViewPanel.Builder<>(StatusBean.class, pageRef) {

            private static final long serialVersionUID = -6809736686861678498L;

            @Override
            protected Component getValueComponent(final String key, final StatusBean bean) {
                if ("status".equalsIgnoreCase(key)) {
                    return StatusUtils.getStatusImagePanel("field", bean.getStatus());
                } else {
                    return super.getValueComponent(key, bean);
                }
            }
        };
        builder.setModel(model);
        builder.setItems(statusBeans);
        builder.includes("resource", "connObjectLink", "status");
        builder.withChecks(ListViewPanel.CheckAvailability.NONE);
        builder.setReuseItem(false);

        ActionLink<StatusBean> connObjectLink = new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                Pair<ConnObject, ConnObject> pair =
                        getConnObjectTOs(bean.getKey(), bean.getResource(), connObjects);
                return pair != null && pair.getRight() != null;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                mlp.next(bean.getResource(), new RemoteAnyPanel(bean, connObjects), target);
            }
        };
        if (!enableConnObjectLink) {
            connObjectLink.disable();
        }
        SyncopeWebApplication.get().getStatusProvider().addConnObjectLink(builder, connObjectLink);

        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return failureReasons.containsKey(bean.getResource());
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                mlp.next(bean.getResource(), new PropagationErrorPanel(failureReasons.get(bean.getResource())), target);
            }
        }, ActionLink.ActionType.PROPAGATION_TASKS, StringUtils.EMPTY);

        listViewPanel = ListViewPanel.class.cast(builder.build(MultilevelPanel.FIRST_LEVEL_ID));
        mlp.setFirstLevel(listViewPanel);
    }

    public void setCheckAvailability(final ListViewPanel.CheckAvailability check) {
        listViewPanel.setCheckAvailability(check);
    }

    public Map<String, StatusBean> getInitialStatusBeanMap() {
        return initialStatusBeanMap;
    }

    protected static Pair<ConnObject, ConnObject> getConnObjectTOs(
            final String anyKey,
            final String resource,
            final List<Triple<ConnObject, ConnObjectWrapper, String>> objects) {

        for (Triple<ConnObject, ConnObjectWrapper, String> object : objects) {
            if (anyKey.equals(object.getMiddle().getAny().getKey())
                    && resource.equalsIgnoreCase(object.getMiddle().getResource())) {

                return Pair.of(object.getLeft(), object.getMiddle().getConnObjectTO());
            }
        }

        return null;
    }

    static class RemoteAnyPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        protected final StatusBean bean;

        protected final List<Triple<ConnObject, ConnObjectWrapper, String>> connObjects;

        RemoteAnyPanel(final StatusBean bean, final List<Triple<ConnObject, ConnObjectWrapper, String>> connObjects) {
            this.bean = bean;
            this.connObjects = connObjects;

            add(new ConnObjectPanel(
                    REMOTE_OBJECT_PANEL_ID,
                    Pair.of(new ResourceModel("before"), new ResourceModel("after")),
                    getConnObjectTOs(),
                    false));
        }

        @Override
        protected final Pair<ConnObject, ConnObject> getConnObjectTOs() {
            return StatusPanel.getConnObjectTOs(bean.getKey(), bean.getResource(), connObjects);
        }
    }
}
