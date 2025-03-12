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
package org.apache.syncope.client.ui.commons.status;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.LabelPanel;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public final class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    public static StatusBean getStatusBean(
            final AnyTO anyTO,
            final String resource,
            final ConnObject connObject,
            final boolean notUser) {

        StatusBean statusBean = new StatusBean(anyTO, resource);

        if (connObject != null) {
            Boolean enabled = isEnabled(connObject);
            statusBean.setStatus(Optional.ofNullable(enabled).map(aBoolean -> aBoolean
                    ? Status.ACTIVE
                    : Status.SUSPENDED).orElseGet(() -> (notUser ? Status.ACTIVE : Status.UNDEFINED)));

            statusBean.setConnObjectLink(getConnObjectLink(connObject));
        }

        return statusBean;
    }

    public static StatusBean getStatusBean(
            final RealmTO realmTO,
            final String resource,
            final ConnObject connObject) {

        StatusBean statusBean = new StatusBean(realmTO, resource);

        if (connObject != null) {
            Boolean enabled = isEnabled(connObject);
            statusBean.setStatus(Optional.ofNullable(enabled)
                    .filter(aBoolean -> !aBoolean).map(aBoolean -> Status.SUSPENDED).orElse(Status.ACTIVE));

            statusBean.setConnObjectLink(getConnObjectLink(connObject));
        }

        return statusBean;
    }

    public static Boolean isEnabled(final ConnObject connObject) {
        return connObject.getAttr(ConnIdSpecialName.ENABLE).
                filter(s -> !s.getValues().isEmpty()).
                map(s -> Boolean.valueOf(s.getValues().getFirst())).
                orElse(Boolean.FALSE);
    }

    public static String getConnObjectLink(final ConnObject connObject) {
        return connObject.getAttr(ConnIdSpecialName.NAME).
                filter(s -> !s.getValues().isEmpty()).
                map(s -> s.getValues().getFirst()).
                orElse(null);
    }

    public static StatusR statusR(final String key, final StatusRType type, final Collection<StatusBean> statuses) {
        StatusR.Builder builder = new StatusR.Builder(key, type).onSyncope(false);
        statuses.forEach(status -> {
            if (Constants.SYNCOPE.equalsIgnoreCase(status.getResource())) {
                builder.onSyncope(true);
            } else {
                builder.resource(status.getResource());
            }
        });

        return builder.build();
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
                clazz = Constants.CREATED_ICON;
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

    public static Panel getStatusImagePanel(final String componentId, final ExecStatus status) {
        return new LabelPanel(componentId, getStatusImage("label", status));
    }

    public static Label getStatusImage(final String componentId, final ExecStatus status) {
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
                title = "Propagation succeeded";
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

    public static Panel getWarningStatusPanel(final String componentId) {
        return new LabelPanel(componentId,
                getLabel("label", "warning icon", "Propagation failed", Constants.WARNING_ICON));
    }

    public static Label getLabel(final String componentId, final String alt, final String title, final String clazz) {
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

    private StatusUtils() {
        // private constructor for static utility class
    }
}
