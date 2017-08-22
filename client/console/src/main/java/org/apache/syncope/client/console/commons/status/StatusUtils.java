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
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.LabelPanel;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    private static final Logger LOG = LoggerFactory.getLogger(StatusUtils.class);

    private final ResourceRestClient restClient = new ResourceRestClient();

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

    public List<ConnObjectWrapper> getConnectorObjects(
            final AnyTO any, final Collection<String> resources) {

        final List<ConnObjectWrapper> objects = new ArrayList<>();

        for (String resourceName : resources) {
            ConnObjectTO objectTO = null;
            try {
                objectTO = restClient.readConnObject(resourceName, any.getType(), any.getKey());
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
            final boolean notUser) {

        final StatusBean statusBean = new StatusBean(anyTO, resourceName);

        if (objectTO != null) {
            final Boolean enabled = isEnabled(objectTO);

            final Status status = enabled == null
                    ? (notUser ? Status.ACTIVE : Status.UNDEFINED)
                    : enabled
                            ? Status.ACTIVE
                            : Status.SUSPENDED;

            String connObjectLink = getConnObjectLink(objectTO);

            statusBean.setStatus(status);
            statusBean.setConnObjectLink(connObjectLink);
        }

        return statusBean;
    }

    public StatusBean getStatusBean(
            final RealmTO anyTO,
            final String resourceName,
            final ConnObjectTO objectTO) {

        final StatusBean statusBean = new StatusBean(anyTO, resourceName);

        if (objectTO != null) {
            final Boolean enabled = isEnabled(objectTO);

            final Status status = enabled == null
                    ? Status.ACTIVE
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
        Optional<AttrTO> status = objectTO.getAttr(ConnIdSpecialName.ENABLE);
        return status.isPresent() && status.get().getValues() != null && !status.get().getValues().isEmpty()
                ? Boolean.valueOf(status.get().getValues().get(0))
                : Boolean.FALSE;
    }

    private String getConnObjectLink(final ConnObjectTO objectTO) {
        Optional<AttrTO> name = objectTO == null ? null : objectTO.getAttr(ConnIdSpecialName.NAME);
        return name.isPresent() && name.get().getValues() != null && !name.get().getValues().isEmpty()
                ? name.get().getValues().get(0)
                : null;
    }

    public static PasswordPatch buildPasswordPatch(final String password, final Collection<StatusBean> statuses) {
        final PasswordPatch.Builder builder = new PasswordPatch.Builder();
        builder.value(password);

        for (StatusBean status : statuses) {
            if (Constants.SYNCOPE.equalsIgnoreCase(status.getResource())) {
                builder.onSyncope(true);
            } else {
                builder.resource(status.getResource());
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

                if ("syncope".equalsIgnoreCase(status.getResource())) {
                    statusPatch.setOnSyncope(true);
                } else {
                    statusPatch.getResources().add(status.getResource());
                }

            }
        }

        return statusPatch;
    }

    public static Panel getStatusImagePanel(final String componentId, final Status status) {
        return new LabelPanel(componentId, getStatusImage("label", status));
    }

    public static Label getStatusImage(final String componentId, final Status status) {
        final String alt, title, clazz;

        switch (status) {

            case NOT_YET_SUBMITTED:
                alt = "undefined icon";
                title = "Not yet submitted";
                clazz = Constants.UNDEFINED_ICON;
                break;

            case ACTIVE:
                alt = "active icon";
                title = "Enabled";
                clazz = Constants.ACTIVE_ICON;
                break;

            case UNDEFINED:
                alt = "undefined icon";
                title = "Undefined status";
                clazz = Constants.UNDEFINED_ICON;
                break;

            case OBJECT_NOT_FOUND:
                alt = "notfound icon";
                title = "Not found";
                clazz = Constants.NOT_FOUND_ICON;
                break;

            case CREATED:
                alt = "created icon";
                title = "Created";
                clazz = Constants.UNDEFINED_ICON;
                break;

            case SUSPENDED:
                alt = "inactive icon";
                title = "Disabled";
                clazz = Constants.SUSPENDED_ICON;
                break;

            default:
                alt = StringUtils.EMPTY;
                title = StringUtils.EMPTY;
                clazz = StringUtils.EMPTY;
                break;
        }

        return getLabel(componentId, alt, title, clazz);
    }

    public static Panel getStatusImagePanel(final String componentId, final PropagationTaskExecStatus status) {
        return new LabelPanel(componentId, getStatusImage("label", status));
    }

    public static Label getStatusImage(final String componentId, final PropagationTaskExecStatus status) {
        final String alt, title, clazz;

        switch (status) {

            case NOT_ATTEMPTED:
                alt = "not attempted";
                title = "Not attempted";
                clazz = Constants.UNDEFINED_ICON;
                break;

            case CREATED:
                alt = "created icon";
                title = "Created";
                clazz = Constants.CREATED_ICON;
                break;

            case SUCCESS:
                alt = "success icon";
                title = "Propagation succeded";
                clazz = Constants.ACTIVE_ICON;
                break;

            case FAILURE:
                alt = "failure icon";
                title = "Propagation failed";
                clazz = Constants.NOT_FOUND_ICON;
                break;

            default:
                alt = StringUtils.EMPTY;
                title = StringUtils.EMPTY;
                clazz = StringUtils.EMPTY;
                break;
        }

        return getLabel(componentId, alt, title, clazz);
    }

    private static Label getLabel(final String componentId, final String alt, final String title, final String clazz) {
        return new Label(componentId, StringUtils.EMPTY) {

            private static final long serialVersionUID = 4755868673082976208L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("alt", alt);
                tag.put("title", title);
                tag.put("class", clazz);
            }
        };
    }
}
