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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
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

    private final Action<T> action;

    public ActionPanel(final IModel<T> model, final Action<T> action) {
        this(Constants.ACTION, model, action);
    }

    public ActionPanel(final String componentId, final IModel<T> model, final Action<T> action) {
        super(componentId);
        setOutputMarkupId(true);
        this.action = action;

        T obj = Optional.ofNullable(model).map(IModel::getObject).orElse(null);

        boolean enabled;
        AbstractLink actionLink;

        if (action.getLink() == null || action.getType() == ActionType.NOT_FOUND) {
            enabled = true;
            actionLink = new IndicatingAjaxLink<Void>(Constants.ACTION) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public boolean isEnabled() {
                    return false;
                }

                @Override
                public void onClick(final AjaxRequestTarget target) {
                }
            };
        } else if (action.getType() == ActionType.EXTERNAL_EDITOR) {
            enabled = action.getLink().isEnabled(obj);
            actionLink = new BookmarkablePageLink<>(
                    Constants.ACTION, action.getLink().getPageClass(), action.getLink().getPageParameters()).
                    setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        } else {
            enabled = action.getLink().isEnabled(obj);

            actionLink = action.isOnConfirm()
                    ? new IndicatingOnConfirmAjaxLink<Void>(
                            Constants.ACTION,
                            StringUtils.isNotBlank(action.getLink().getConfirmMessage())
                            ? action.getLink().getConfirmMessage()
                            : Constants.CONFIRM_DELETE, enabled) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    beforeOnClick(target);
                    action.getLink().onClick(target, obj);
                }

                @Override
                public String getAjaxIndicatorMarkupId() {
                    return disableIndicator || !action.getLink().isIndicatorEnabled()
                            ? StringUtils.EMPTY : Constants.VEIL_INDICATOR_MARKUP_ID;
                }
            }
                    : new IndicatingAjaxLink<Void>(Constants.ACTION) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    beforeOnClick(target);
                    action.getLink().onClick(target, obj);
                }

                @Override
                public String getAjaxIndicatorMarkupId() {
                    return disableIndicator || !action.getLink().isIndicatorEnabled()
                            ? StringUtils.EMPTY : Constants.VEIL_INDICATOR_MARKUP_ID;
                }
            };
        }

        if (SyncopeConsoleSession.get().owns(action.getEntitlements(), action.getRealms())) {
            MetaDataRoleAuthorizationStrategy.authorizeAll(actionLink, RENDER);
        } else {
            MetaDataRoleAuthorizationStrategy.unauthorizeAll(actionLink, RENDER);
        }

        actionLink.setVisible(enabled);

        actionIcon = new Label("actionIcon", "");
        actionLink.add(actionIcon);

        String clazz = action.getType().name().toLowerCase() + ".class";
        actionIcon.add(new AttributeModifier("class", new ResourceModel(clazz, clazz)));

        String title = action.getType().name().toLowerCase() + ".title";
        IModel<String> titleModel = new ResourceModel(title, title);
        actionIcon.add(new AttributeModifier("title", titleModel));

        String alt = action.getType().name().toLowerCase() + ".alt";
        actionIcon.add(new AttributeModifier("alt", new ResourceModel(alt, alt)));

        actionLabel = new Label("label", titleModel);
        actionLink.add(actionLabel);
        add(actionLink);

        // ---------------------------
        // Action configuration
        // ---------------------------
        actionLabel.setVisible(action.isVisibleLabel());

        Optional.ofNullable(action.getLabel()).ifPresent(actionLabel::setDefaultModel);

        Optional.ofNullable(action.getTitle()).ifPresent(t -> actionIcon.add(new AttributeModifier("title", t)));

        Optional.ofNullable(action.getAlt()).ifPresent(a -> actionIcon.add(new AttributeModifier("alt", a)));

        Optional.ofNullable(action.getIcon()).ifPresent(i -> actionIcon.add(new AttributeModifier("class", i)));

        this.disableIndicator = !action.hasIndicator();
        // ---------------------------
    }

    protected void beforeOnClick(final AjaxRequestTarget target) {
        switch (this.action.getType()) {
            case DELETE:
            case CREATE:
            case MEMBERS:
            case MAPPING:
            case SET_LATEST_SYNC_TOKEN:
            case REMOVE_SYNC_TOKEN:
            case EDIT_APPROVAL:
            case CLAIM:
                send(this, Broadcast.BUBBLE, new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                break;
            default:
                break;
        }
    }
}
