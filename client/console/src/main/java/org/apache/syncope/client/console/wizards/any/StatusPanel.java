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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SerializableTransformer;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.Status;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.panels.RemoteObjectPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusPanel extends Panel {

    private static final long serialVersionUID = -4013796607157549641L;

    private static final Logger LOG = LoggerFactory.getLogger(StatusPanel.class);

    private Map<String, StatusBean> initialStatusBeanMap;

    private final StatusUtils statusUtils;

    private ListViewPanel<?> listViewPanel;

    public <T extends AnyTO> StatusPanel(
            final String id,
            final T any,
            final IModel<List<StatusBean>> model,
            final PageReference pageRef) {
        super(id);
        statusUtils = new StatusUtils();
        init(any, model,
                CollectionUtils.collect(statusUtils.getConnectorObjects(any),
                        new SerializableTransformer<ConnObjectWrapper, Pair<ConnObjectTO, ConnObjectWrapper>>() {

                    private static final long serialVersionUID = 2658691884036294287L;

                    @Override
                    public Pair<ConnObjectTO, ConnObjectWrapper> transform(final ConnObjectWrapper input) {
                        return Pair.of(null, input);
                    }

                }, new ArrayList<Pair<ConnObjectTO, ConnObjectWrapper>>()), pageRef, false);
    }

    public <T extends AnyTO> StatusPanel(
            final String id,
            final T any,
            final IModel<List<StatusBean>> model,
            final List<Pair<ConnObjectTO, ConnObjectWrapper>> connObjects,
            final PageReference pageRef) {
        super(id);
        statusUtils = new StatusUtils();
        init(any, model, connObjects, pageRef, true);
    }

    private void init(
            final AnyTO any,
            final IModel<List<StatusBean>> model,
            final List<Pair<ConnObjectTO, ConnObjectWrapper>> connObjects,
            final PageReference pageRef,
            final boolean enableConnObjectLink) {

        final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size() + 1);
        initialStatusBeanMap = new LinkedHashMap<>(connObjects.size() + 1);

        final StatusBean syncope = new StatusBean(any, Constants.SYNCOPE);

        if (any instanceof UserTO) {
            syncope.setConnObjectLink(((UserTO) any).getUsername());

            Status syncopeStatus = Status.UNDEFINED;
            if (((UserTO) any).getStatus() != null) {
                try {
                    syncopeStatus = Status.valueOf(((UserTO) any).getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unexpected status found: {}", ((UserTO) any).getStatus(), e);
                }
            }
            syncope.setStatus(syncopeStatus);
        } else if (any instanceof GroupTO) {
            syncope.setConnObjectLink(((GroupTO) any).getName());
            syncope.setStatus(Status.ACTIVE);
        }

        statusBeans.add(syncope);
        initialStatusBeanMap.put(syncope.getResourceName(), syncope);

        for (Pair<ConnObjectTO, ConnObjectWrapper> pair : connObjects) {
            ConnObjectWrapper entry = pair.getRight();
            final StatusBean statusBean = statusUtils.getStatusBean(entry.getAny(),
                    entry.getResourceName(),
                    entry.getConnObjectTO(),
                    any instanceof GroupTO);

            initialStatusBeanMap.put(entry.getResourceName(), statusBean);
            statusBeans.add(statusBean);
        }

        final MultilevelPanel mlp = new MultilevelPanel("resources");
        add(mlp);

        ListViewPanel.Builder<StatusBean> builder = new ListViewPanel.Builder<StatusBean>(StatusBean.class, pageRef) {

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
        builder.includes("resourceName", "connObjectLink", "status");
        builder.withChecks(ListViewPanel.CheckAvailability.NONE);
        builder.setReuseItem(false);

        final ActionLink<StatusBean> connObjectLink = new ActionLink<StatusBean>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                final Pair<ConnObjectTO, ConnObjectTO> pair
                        = getConnObjectTO(bean.getAnyKey(), bean.getResourceName(), connObjects);

                return pair != null && pair.getRight() != null;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                mlp.next(bean.getResourceName(), new RemoteAnyPanel(bean, connObjects), target);
            }
        };

        if (!enableConnObjectLink) {
            connObjectLink.disable();
        }

        builder.addAction(connObjectLink, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT);

        listViewPanel = ListViewPanel.class.cast(builder.build(MultilevelPanel.FIRST_LEVEL_ID));
        mlp.setFirstLevel(listViewPanel);
    }

    public void setCheckAvailability(final ListViewPanel.CheckAvailability check) {
        listViewPanel.setCheckAvailability(check);
    }

    public Map<String, StatusBean> getInitialStatusBeanMap() {
        return initialStatusBeanMap;
    }

    protected Pair<ConnObjectTO, ConnObjectTO> getConnObjectTO(
            final String anyKey, final String resourceName,
            final List<Pair<ConnObjectTO, ConnObjectWrapper>> objects) {

        for (Pair<ConnObjectTO, ConnObjectWrapper> object : objects) {
            if (anyKey.equals(object.getRight().getAny().getKey())
                    && resourceName.equalsIgnoreCase(object.getRight().getResourceName())) {

                return Pair.of(object.getLeft(), object.getRight().getConnObjectTO());
            }
        }

        return null;
    }

    public class RemoteAnyPanel extends RemoteObjectPanel {

        private static final long serialVersionUID = 4303365227411467563L;

        private final StatusBean bean;

        private final List<Pair<ConnObjectTO, ConnObjectWrapper>> connObjects;

        public RemoteAnyPanel(final StatusBean bean, final List<Pair<ConnObjectTO, ConnObjectWrapper>> connObjects) {
            this.bean = bean;
            this.connObjects = connObjects;

            add(new ConnObjectPanel(REMOTE_OBJECT_PANEL_ID, getConnObjectTO(), false));
        }

        @Override
        protected final Pair<ConnObjectTO, ConnObjectTO> getConnObjectTO() {
            return StatusPanel.this.getConnObjectTO(bean.getAnyKey(), bean.getResourceName(), connObjects);
        }
    }
}
