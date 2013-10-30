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
package org.apache.syncope.console.commons.status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.ImagePanel;
import org.apache.syncope.console.pages.panels.StatusPanel;
import org.apache.syncope.console.rest.AbstractAttributableRestClient;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StatusUtils.class);

    public static final String IMG_STATUES = "../statuses/";

    private final AbstractAttributableRestClient restClient;

    public StatusUtils(final AbstractAttributableRestClient restClient) {
        this.restClient = restClient;
    }

    public List<ConnObjectWrapper> getConnectorObjects(final AbstractAttributableTO attributable) {
        final List<ConnObjectWrapper> objects = new ArrayList<ConnObjectWrapper>();
        objects.addAll(getConnectorObjects(attributable, attributable.getResources()));
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
            final AbstractAttributableTO attributable, final Collection<String> resources) {

        final List<ConnObjectWrapper> objects = new ArrayList<ConnObjectWrapper>();

        for (String resourceName : resources) {
            ConnObjectTO objectTO = null;
            try {
                objectTO = restClient.getConnectorObject(resourceName, attributable.getId());
            } catch (Exception e) {
                LOG.warn("ConnObject '{}' not found on resource '{}'", attributable.getId(), resourceName);
            }

            objects.add(new ConnObjectWrapper(attributable, resourceName, objectTO));
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

            final Status status = enabled == null
                    ? (isRole ? Status.ACTIVE : Status.UNDEFINED)
                    : enabled
                    ? Status.ACTIVE
                    : Status.SUSPENDED;

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

    public static StatusMod buildStatusMod(final Collection<StatusBean> statuses) {
        return buildStatusMod(statuses, null);
    }

    public static StatusMod buildStatusMod(final Collection<StatusBean> statuses, final Boolean enable) {
        StatusMod statusMod = new StatusMod();
        statusMod.setOnSyncope(false);

        for (StatusBean status : statuses) {
            if (enable == null
                    || (enable && !status.getStatus().isActive()) || (!enable && status.getStatus().isActive())) {

                if ("Syncope".equals(status.getResourceName())) {
                    statusMod.setOnSyncope(true);
                } else {
                    statusMod.getResourceNames().add(status.getResourceName());
                }

            }
        }

        return statusMod;
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
                        statusBean.setStatus(Status.NOT_YET_SUBMITTED);
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
            if (attributableId.equals(object.getAttributable().getId())
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
                statusName = Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Not yet submitted";
                break;

            case ACTIVE:
                statusName = Status.ACTIVE.toString();
                alt = "active icon";
                title = "Enabled";
                break;

            case UNDEFINED:
                statusName = Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Undefined status";
                break;

            case OBJECT_NOT_FOUND:
                statusName = Status.OBJECT_NOT_FOUND.toString();
                alt = "notfound icon";
                title = "Not found";
                break;

            default:
                statusName = Status.SUSPENDED.toString();
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
                statusName = Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Not yet submitted";
                break;

            case ACTIVE:
                statusName = Status.ACTIVE.toString();
                alt = "active icon";
                title = "Enabled";
                break;

            case UNDEFINED:
                statusName = Status.UNDEFINED.toString();
                alt = "undefined icon";
                title = "Undefined status";
                break;

            case OBJECT_NOT_FOUND:
                statusName = Status.OBJECT_NOT_FOUND.toString();
                alt = "notfound icon";
                title = "Not found";
                break;

            default:
                statusName = Status.SUSPENDED.toString();
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
}
