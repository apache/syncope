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
package org.apache.syncope.console.commons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.PropagationTargetsTO;
import org.apache.syncope.console.pages.panels.ImagePanel;
import org.apache.syncope.console.pages.panels.StatusPanel;
import org.apache.syncope.console.rest.AbstractAttributableRestClient;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    public static final String IMG_STATUES = "../statuses/";

    public enum Status {

        NOT_YET_SUBMITTED(""),
        CREATED("created"),
        ACTIVE("active"),
        SUSPENDED("inactive"),
        UNDEFINED("undefined"),
        OBJECT_NOT_FOUND("objectnotfound");

        public boolean isActive() {
            return this == ACTIVE;
        }

        private Status(final String name) {
            this.name = name;
        }

        private final String name;

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StatusUtils.class);

    private final AbstractAttributableRestClient restClient;

    public StatusUtils(final AbstractAttributableRestClient restClient) {
        this.restClient = restClient;
    }

    public List<ConnObjectWrapper> getConnectorObjects(final Collection<AbstractAttributableTO> attributables) {
        final List<ConnObjectWrapper> objects = new ArrayList<ConnObjectWrapper>();

        for (AbstractAttributableTO attributableTO : attributables) {
            objects.addAll(getConnectorObjects(attributableTO, attributableTO.getResources()));
        }

        return objects;
    }

    public List<ConnObjectWrapper> getConnectorObjects(
            final Collection<AbstractAttributableTO> attributables, final Collection<String> resources) {
        final List<ConnObjectWrapper> objects = new ArrayList<ConnObjectWrapper>();

        for (AbstractAttributableTO attributableTO : attributables) {
            objects.addAll(getConnectorObjects(attributableTO, resources));
        }

        return objects;
    }

    private List<ConnObjectWrapper> getConnectorObjects(
            final AbstractAttributableTO attributableTO, final Collection<String> resources) {
        final List<ConnObjectWrapper> objects = new ArrayList<ConnObjectWrapper>();

        for (String resourceName : resources) {
            ConnObjectTO objectTO = null;
            try {
                objectTO = restClient.getConnectorObject(resourceName, attributableTO.getId());
            } catch (Exception e) {
                LOG.warn("ConnObject '{}' not found on resource '{}'", attributableTO.getId(), resourceName);
            }

            objects.add(new ConnObjectWrapper(attributableTO, resourceName, objectTO));
        }

        return objects;
    }

    public StatusBean getStatusBean(
            final AbstractAttributableTO attributable,
            final String resourceName,
            final ConnObjectTO objectTO,
            final boolean isRole) {
        final StatusBean statusBean = new StatusBean(attributable, resourceName);

        if (objectTO != null) {
            final Boolean enabled = isEnabled(objectTO);

            final StatusUtils.Status status = enabled == null
                    ? (isRole ? StatusUtils.Status.ACTIVE : StatusUtils.Status.UNDEFINED)
                    : enabled
                    ? StatusUtils.Status.ACTIVE
                    : StatusUtils.Status.SUSPENDED;

            final String accountLink = getAccountLink(objectTO);

            statusBean.setStatus(status);
            statusBean.setAccountLink(accountLink);
        }

        return statusBean;
    }

    private Boolean isEnabled(final ConnObjectTO objectTO) {
        final Map<String, AttributeTO> attributeTOs = objectTO.getAttrMap();

        final AttributeTO status = attributeTOs.get(ConnIdSpecialAttributeName.ENABLE);

        return status != null && status.getValues() != null && !status.getValues().isEmpty()
                ? Boolean.parseBoolean(status.getValues().get(0))
                : null;
    }

    private String getAccountLink(final ConnObjectTO objectTO) {
        final Map<String, AttributeTO> attributeTOs = objectTO == null
                ? Collections.<String, AttributeTO>emptyMap()
                : objectTO.getAttrMap();

        final AttributeTO name = attributeTOs.get(ConnIdSpecialAttributeName.NAME);

        return name != null && name.getValues() != null && !name.getValues().isEmpty()
                ? name.getValues().get(0)
                : null;
    }

    public static PropagationRequestTO buildPropagationRequestTO(final Collection<StatusBean> statuses) {
        return buildPropagationRequestTO(statuses, null);
    }

    public static PropagationRequestTO buildPropagationRequestTO(final Collection<StatusBean> statuses,
            final Boolean enable) {

        PropagationRequestTO propagationRequestTO = new PropagationRequestTO();

        for (StatusBean status : statuses) {
            if (enable == null
                    || (enable && !status.getStatus().isActive()) || (!enable && status.getStatus().isActive())) {

                if ("Syncope".equals(status.getResourceName())) {
                    propagationRequestTO.setOnSyncope(true);
                } else {
                    propagationRequestTO.getResources().add(status.getResourceName());
                }

            }
        }

        return propagationRequestTO;
    }

    public static PropagationTargetsTO buildPropagationTargetsTO(final Collection<StatusBean> statuses) {

        final PropagationTargetsTO propagationTargetsTO = new PropagationTargetsTO();

        for (StatusBean status : statuses) {
            propagationTargetsTO.getResources().add(status.getResourceName());
        }

        return propagationTargetsTO;
    }

    public static void update(
            final AbstractAttributableTO attributable,
            final StatusPanel statusPanel,
            final AjaxRequestTarget target,
            final Collection<String> resourcesToAdd,
            final Collection<String> resourcesToRemove) {

        if (statusPanel != null) {
            Map<String, StatusBean> statusMap = new LinkedHashMap<String, StatusBean>();
            for (StatusBean statusBean : statusPanel.getStatusBeans()) {
                statusMap.put(statusBean.getResourceName(), statusBean);
            }

            for (String resourceName : resourcesToAdd) {
                if (!statusMap.keySet().contains(resourceName)) {
                    StatusBean statusBean;
                    if (statusPanel.getInitialStatusBeanMap().containsKey(resourceName)) {
                        statusBean = statusPanel.getInitialStatusBeanMap().get(resourceName);
                    } else {
                        statusBean = new StatusBean(attributable, resourceName);
                        statusBean.setStatus(StatusUtils.Status.NOT_YET_SUBMITTED);
                    }

                    statusMap.put(statusBean.getResourceName(), statusBean);
                }
            }

            for (String resource : resourcesToRemove) {
                statusMap.remove(resource);
            }

            statusPanel.updateStatusBeans(new ArrayList<StatusBean>(statusMap.values()));
            target.add(statusPanel);
        }
    }

    public ConnObjectTO getConnObjectTO(
            final Long attributableId, final String resourceName, final List<ConnObjectWrapper> objects) {

        for (ConnObjectWrapper object : objects) {
            if (attributableId.equals(object.getAttributable())
                    && resourceName.equalsIgnoreCase(object.getResourceName())) {
                return object.getConnObjectTO();
            }
        }

        return null;
    }

    public Image getStatusImage(final String componentId, final Status status) {
        final String alt, title, statusName;

        switch (status) {

            case NOT_YET_SUBMITTED:
                statusName = StatusUtils.Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Not yet submitted";
                break;

            case ACTIVE:
                statusName = StatusUtils.Status.ACTIVE.toString();
                alt = "active icon";
                title = "Enabled";
                break;

            case UNDEFINED:
                statusName = StatusUtils.Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Undefined status";
                break;

            case OBJECT_NOT_FOUND:
                statusName = StatusUtils.Status.OBJECT_NOT_FOUND.toString();
                alt = "notfound icon";
                title = "Not found";
                break;

            default:
                statusName = StatusUtils.Status.SUSPENDED.toString();
                alt = "inactive icon";
                title = "Disabled";
        }

        final Image img = new Image(componentId, IMG_STATUES + statusName + Constants.PNG_EXT);

        img.add(new Behavior() {

            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {
                tag.put("alt", alt);
                tag.put("title", title);
            }
        });

        return img;
    }

    public ImagePanel getStatusImagePanel(final String componentId, final Status status) {
        final String alt, title, statusName;

        switch (status) {

            case NOT_YET_SUBMITTED:
                statusName = StatusUtils.Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Not yet submitted";
                break;

            case ACTIVE:
                statusName = StatusUtils.Status.ACTIVE.toString();
                alt = "active icon";
                title = "Enabled";
                break;

            case UNDEFINED:
                statusName = StatusUtils.Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Undefined status";
                break;

            case OBJECT_NOT_FOUND:
                statusName = StatusUtils.Status.OBJECT_NOT_FOUND.toString();
                alt = "notfound icon";
                title = "Not found";
                break;

            default:
                statusName = StatusUtils.Status.SUSPENDED.toString();
                alt = "inactive icon";
                title = "Disabled";
        }

        final ImagePanel imagePanel = new ImagePanel(componentId, IMG_STATUES + statusName + Constants.PNG_EXT);
        imagePanel.add(new Behavior() {

            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {
                tag.put("alt", alt);
                tag.put("title", title);
            }
        });

        return imagePanel;
    }

    public static class ConnObjectWrapper implements Serializable {

        private static final long serialVersionUID = 9083721948999924299L;

        private final AbstractAttributableTO attributable;

        private final String resourceName;

        private final ConnObjectTO connObjectTO;

        public ConnObjectWrapper(AbstractAttributableTO attributable, String resourceName, ConnObjectTO connObjectTO) {
            this.attributable = attributable;
            this.resourceName = resourceName;
            this.connObjectTO = connObjectTO;
        }

        public AbstractAttributableTO getAttributable() {
            return attributable;
        }

        public String getResourceName() {
            return resourceName;
        }

        public ConnObjectTO getConnObjectTO() {
            return connObjectTO;
        }
    }

    public static abstract class StatusBeanProvider extends SortableDataProvider<StatusBean, String> {

        private static final long serialVersionUID = 4287357360778016173L;

        private SortableDataProviderComparator<StatusBean> comparator;

        public StatusBeanProvider(final String sort) {
            //Default sorting
            setSort(sort, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<StatusBean>(this);
        }

        @Override
        public Iterator<StatusBean> iterator(final long first, final long count) {
            List<StatusBean> list = getStatusBeans();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return getStatusBeans().size();
        }

        @Override
        public IModel<StatusBean> model(final StatusBean resource) {
            return new AbstractReadOnlyModel<StatusBean>() {

                private static final long serialVersionUID = -7802635613997243712L;

                @Override
                public StatusBean getObject() {
                    return resource;
                }
            };
        }

        public abstract List<StatusBean> getStatusBeans();
    }
}
