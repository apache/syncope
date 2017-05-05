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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * This empty class must exist because there not seems to be alternative to provide specialized HTML for edit links.
 *
 * @param <T> model object type.
 */
public final class ActionPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 322966537010107771L;

    private final Label actionIcon;

    private final Label actionLabel;

    private boolean disableIndicator = false;

    public ActionPanel(final IModel<T> model, final Action<T> action) {
        this("action", model, action);
    }

    public ActionPanel(final String componentId, final IModel<T> model, final Action<T> action) {
        super(componentId);
        setOutputMarkupId(true);

        final T obj;
        if (model == null) {
            obj = null;
        } else {
            obj = model.getObject();
        }

        final boolean enabled;
        final AjaxLink<Void> actionLink;

        if (action.getLink() == null || action.getType() == ActionType.NOT_FOUND) {
            enabled = true;
            actionLink = new IndicatingAjaxLink<Void>("action") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public boolean isEnabled() {
                    return false;
                }

                @Override
                public void onClick(final AjaxRequestTarget target) {
                }
            };
        } else {
            enabled = action.getLink().isEnabled(obj);

            actionLink = action.isOnConfirm()
                    ? new IndicatingOnConfirmAjaxLink<Void>("action", enabled) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    action.getLink().onClick(target, obj);
                }

                @Override
                public String getAjaxIndicatorMarkupId() {
                    return disableIndicator || !action.getLink().isIndicatorEnabled()
                            ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                }
            }
                    : new IndicatingAjaxLink<Void>("action") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    action.getLink().onClick(target, obj);
                }

                @Override
                public String getAjaxIndicatorMarkupId() {
                    return disableIndicator || !action.getLink().isIndicatorEnabled()
                            ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                }
            };
        }

        actionLink.setVisible(enabled);

        actionIcon = new Label("actionIcon", "");
        actionLink.add(actionIcon);

        final String clazz = action.getType().name().toLowerCase() + ".class";
        actionIcon.add(new AttributeModifier("class", new ResourceModel(clazz, clazz)));

        final String title = action.getType().name().toLowerCase() + ".title";
        final IModel<String> titleModel = new ResourceModel(title, title);
        actionIcon.add(new AttributeModifier("title", titleModel));

        final String alt = action.getType().name().toLowerCase() + ".alt";
        actionIcon.add(new AttributeModifier("alt", new ResourceModel(alt, alt)));

        actionLabel = new Label("label", titleModel);
        actionLink.add(actionLabel);
        add(actionLink);

        // ---------------------------
        // Action configuration
        // ---------------------------
        actionLabel.setVisible(action.isVisibleLabel());

        if (action.getLabel() != null) {
            actionLabel.setDefaultModel(action.getLabel());
        }

        if (action.getTitle() != null) {
            actionIcon.add(new AttributeModifier("title", action.getTitle()));
        }

        if (action.getAlt() != null) {
            actionIcon.add(new AttributeModifier("alt", action.getAlt()));
        }

        if (action.getIcon() != null) {
            actionIcon.add(new AttributeModifier("class", action.getIcon()));
        }

        this.disableIndicator = !action.hasIndicator();
        // ---------------------------
    }
}
