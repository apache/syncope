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
package org.apache.syncope.client.console.commons.status;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ImagePanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    private static final Logger LOG = LoggerFactory.getLogger(StatusUtils.class);

    private static final String IMG_PREFIX = "/img/statuses/";

    private final AbstractAnyRestClient<?> restClient;

    public StatusUtils(final AbstractAnyRestClient<?> restClient) {
        this.restClient = restClient;
    }

    public List<ConnObjectWrapper> getConnectorObjects(final AnyTO any) {
        final List<ConnObjectWrapper> objects = new ArrayList<>();
        objects.addAll(getConnectorObjects(any, any.getResources()));
        return objects;
    }

    public List<ConnObjectWrapper> getConnectorObjects(
            final Collection<AnyTO> anys, final Collection<String> resources) {

        final List<ConnObjectWrapper> objects = new ArrayList<>();

        for (AnyTO any : anys) {
            objects.addAll(getConnectorObjects(any, resources));
        }

        return objects;
    }

    private List<ConnObjectWrapper> getConnectorObjects(
            final AnyTO any, final Collection<String> resources) {

        final List<ConnObjectWrapper> objects = new ArrayList<>();

        for (String resourceName : resources) {
            ConnObjectTO objectTO = null;
            try {
                objectTO = restClient.readConnObject(resourceName, any.getKey());
            } catch (Exception e) {
                LOG.warn("ConnObject '{}' not found on resource '{}'", any.getKey(), resourceName);
            }

            objects.add(new ConnObjectWrapper(any, resourceName, objectTO));
        }

        return objects;
    }

    public StatusBean getStatusBean(
            final AnyTO anyTO,
            final String resourceName,
            final ConnObjectTO objectTO,
            final boolean isGroup) {

        final StatusBean statusBean = new StatusBean(anyTO, resourceName);

        if (objectTO != null) {
            final Boolean enabled = isEnabled(objectTO);

            final Status status = enabled == null
                    ? (isGroup ? Status.ACTIVE : Status.UNDEFINED)
                    : enabled
                            ? Status.ACTIVE
                            : Status.SUSPENDED;

            String connObjectLink = getConnObjectLink(objectTO);

            statusBean.setStatus(status);
            statusBean.setConnObjectLink(connObjectLink);
        }

        return statusBean;
    }

    private Boolean isEnabled(final ConnObjectTO objectTO) {
        final Map<String, AttrTO> attributeTOs = objectTO.getPlainAttrMap();

        final AttrTO status = attributeTOs.get(ConnIdSpecialAttributeName.ENABLE);

        return status != null && status.getValues() != null && !status.getValues().isEmpty()
                ? Boolean.parseBoolean(status.getValues().get(0))
                : null;
    }

    private String getConnObjectLink(final ConnObjectTO objectTO) {
        final Map<String, AttrTO> attributeTOs = objectTO == null
                ? Collections.<String, AttrTO>emptyMap()
                : objectTO.getPlainAttrMap();

        final AttrTO name = attributeTOs.get(ConnIdSpecialAttributeName.NAME);

        return name != null && name.getValues() != null && !name.getValues().isEmpty()
                ? name.getValues().get(0)
                : null;
    }

    public static PasswordPatch buildPasswordPatch(final String password, final Collection<StatusBean> statuses) {
        final PasswordPatch.Builder builder = new PasswordPatch.Builder();
        builder.value(password);

        for (StatusBean status : statuses) {
            if ("syncope".equalsIgnoreCase(status.getResourceName())) {
                builder.onSyncope(true);
            } else {
                builder.resource(status.getResourceName());
            }
        }
        return builder.build();
    }

    public static StatusPatch buildStatusPatch(final Collection<StatusBean> statuses) {
        return buildStatusPatch(statuses, null);
    }

    public static StatusPatch buildStatusPatch(final Collection<StatusBean> statuses, final Boolean enable) {
        StatusPatch statusPatch = new StatusPatch();
        statusPatch.setOnSyncope(false);

        for (StatusBean status : statuses) {
            if (enable == null
                    || (enable && !status.getStatus().isActive()) || (!enable && status.getStatus().isActive())) {

                if ("syncope".equalsIgnoreCase(status.getResourceName())) {
                    statusPatch.setOnSyncope(true);
                } else {
                    statusPatch.getResources().add(status.getResourceName());
                }

            }
        }

        return statusPatch;
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

        final Image img = new Image(componentId,
                new ContextRelativeResource(IMG_PREFIX + statusName + Constants.PNG_EXT));
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

        final ImagePanel imagePanel = new ImagePanel(componentId,
                new ContextRelativeResource(IMG_PREFIX + statusName + Constants.PNG_EXT));
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
