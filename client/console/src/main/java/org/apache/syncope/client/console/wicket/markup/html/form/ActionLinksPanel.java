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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * This empty class must exist because there not seems to be alternative to provide specialized HTML for edit links.
 *
 * @param <T> model object type.
 */
public final class ActionLinksPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 322966537010107771L;

    private final IModel<T> model;

    private boolean disableIndicator = false;

    private ActionLinksPanel(final String componentId, final IModel<T> model) {
        super(componentId, model);
        this.model = model;

        setOutputMarkupId(true);

        super.add(new Fragment("panelClaim", "emptyFragment", this));
        super.add(new Fragment("panelManageResources", "emptyFragment", this));
        super.add(new Fragment("panelManageUsers", "emptyFragment", this));
        super.add(new Fragment("panelManageGroups", "emptyFragment", this));
        super.add(new Fragment("panelMapping", "emptyFragment", this));
        super.add(new Fragment("panelMustChangePassword", "emptyFragment", this));
        super.add(new Fragment("panelResetTime", "emptyFragment", this));
        super.add(new Fragment("panelClone", "emptyFragment", this));
        super.add(new Fragment("panelCreate", "emptyFragment", this));
        super.add(new Fragment("panelEdit", "emptyFragment", this));
        super.add(new Fragment("panelHtmlEdit", "emptyFragment", this));
        super.add(new Fragment("panelTextEdit", "emptyFragment", this));
        super.add(new Fragment("panelReset", "emptyFragment", this));
        super.add(new Fragment("panelEnable", "emptyFragment", this));
        super.add(new Fragment("panelNotFound", "emptyFragment", this));
        super.add(new Fragment("panelView", "emptyFragment", this));
        super.add(new Fragment("panelSearch", "emptyFragment", this));
        super.add(new Fragment("panelDelete", "emptyFragment", this));
        super.add(new Fragment("panelExecute", "emptyFragment", this));
        super.add(new Fragment("panelDryRun", "emptyFragment", this));
        super.add(new Fragment("panelSelect", "emptyFragment", this));
        super.add(new Fragment("panelClose", "emptyFragment", this));
        super.add(new Fragment("panelExport", "emptyFragment", this));
        super.add(new Fragment("panelSuspend", "emptyFragment", this));
        super.add(new Fragment("panelReactivate", "emptyFragment", this));
        super.add(new Fragment("panelReload", "emptyFragment", this));
        super.add(new Fragment("panelChangeView", "emptyFragment", this));
        super.add(new Fragment("panelUnlink", "emptyFragment", this));
        super.add(new Fragment("panelLink", "emptyFragment", this));
        super.add(new Fragment("panelUnassign", "emptyFragment", this));
        super.add(new Fragment("panelAssign", "emptyFragment", this));
        super.add(new Fragment("panelDeprovision", "emptyFragment", this));
        super.add(new Fragment("panelProvision", "emptyFragment", this));
        super.add(new Fragment("panelPropagationTasks", "emptyFragment", this));
        super.add(new Fragment("panelZoomIn", "emptyFragment", this));
        super.add(new Fragment("panelZoomOut", "emptyFragment", this));
    }

    public ActionLinksPanel<T> add(
            final ActionLink<T> link,
            final ActionLink.ActionType type,
            final String entitlements,
            final boolean enabled) {

        Fragment fragment = null;

        switch (type) {

            case CLAIM:
                fragment = new Fragment("panelClaim", "fragmentClaim", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("claimLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case MANAGE_RESOURCES:
                fragment = new Fragment("panelManageResources", "fragmentManageResources", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("manageResourcesLink") {

                    private static final long serialVersionUID = -6957616042924610291L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case MANAGE_USERS:
                fragment = new Fragment("panelManageUsers", "fragmentManageUsers", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("manageUsersLink") {

                    private static final long serialVersionUID = -6957616042924610292L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case MANAGE_GROUPS:
                fragment = new Fragment("panelManageGroups", "fragmentManageGroups", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("manageGroupsLink") {

                    private static final long serialVersionUID = -6957616042924610293L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case MAPPING:
                fragment = new Fragment("panelMapping", "fragmentMapping", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("mappingLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case MUSTCHANGEPASSWORD:
                fragment = new Fragment("panelMustChangePassword", "fragmentMustChangePassword", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("MustChangePasswordLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case RESET_TIME:
                fragment = new Fragment("panelResetTime", "fragmentResetTime", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("resetTimeLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case CLONE:
                fragment = new Fragment("panelClone", "fragmentClone", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("cloneLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case CREATE:
                fragment = new Fragment("panelCreate", "fragmentCreate", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("createLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case RESET:
                fragment = new Fragment("panelReset", "fragmentReset", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("resetLink") {

                    private static final long serialVersionUID = -6957616042924610290L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case EDIT:
                fragment = new Fragment("panelEdit", "fragmentEdit", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("editLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case HTML_EDIT:
                fragment = new Fragment("panelHtmlEdit", "fragmentHtmlEdit", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("htmlEditLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case TEXT_EDIT:
                fragment = new Fragment("panelTextEdit", "fragmentTextEdit", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("textEditLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case ENABLE:
                fragment = new Fragment("panelEnable", "fragmentEnable", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("enableLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case NOT_FOND:
                fragment = new Fragment("panelNotFound", "fragmentNotFound", this);
                break;

            case VIEW:
                fragment = new Fragment("panelView", "fragmentView", this);
                fragment.addOrReplace(new IndicatingAjaxLink<Void>("viewLink") {

                    private static final long serialVersionUID = -1876519166660008562L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case SEARCH:
                fragment = new Fragment("panelSearch", "fragmentSearch", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("searchLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case EXECUTE:
                fragment = new Fragment("panelExecute", "fragmentExecute", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("executeLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case DRYRUN:
                fragment = new Fragment("panelDryRun", "fragmentDryRun", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("dryRunLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case DELETE:
                fragment = new Fragment("panelDelete", "fragmentDelete", this);

                fragment.addOrReplace(new IndicatingOnConfirmAjaxLink<Void>("deleteLink", enabled) {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }).setVisible(link.isEnabled(model.getObject()));

                break;

            case SELECT:
                fragment = new Fragment("panelSelect", "fragmentSelect", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("selectLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }).setVisible(link.isEnabled(model.getObject()));

                break;
            case CLOSE:
                fragment = new Fragment("panelClose", "fragmentClose", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("closeLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }).setVisible(link.isEnabled(model.getObject()));

                break;

            case EXPORT:
                fragment = new Fragment("panelExport", "fragmentExport", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("exportLink") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case SUSPEND:
                fragment = new Fragment("panelSuspend", "fragmentSuspend", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("suspendLink") {

                    private static final long serialVersionUID = -6957616042924610291L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case REACTIVATE:
                fragment = new Fragment("panelReactivate", "fragmentReactivate", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("reactivateLink") {

                    private static final long serialVersionUID = -6957616042924610292L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case RELOAD:
                fragment = new Fragment("panelReload", "fragmentReload", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("reloadLink") {

                    private static final long serialVersionUID = -6957616042924610293L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case CHANGE_VIEW:
                fragment = new Fragment("panelChangeView", "fragmentChangeView", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("changeViewLink") {

                    private static final long serialVersionUID = -6957616042924610292L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case UNLINK:
                fragment = new Fragment("panelUnlink", "fragmentUnlink", this);

                fragment.addOrReplace(new IndicatingOnConfirmAjaxLink<Void>("unlinkLink", "confirmUnlink", enabled) {

                    private static final long serialVersionUID = -6957616042924610293L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case LINK:
                fragment = new Fragment("panelLink", "fragmentLink", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("linkLink") {

                    private static final long serialVersionUID = -6957616042924610303L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case UNASSIGN:
                fragment = new Fragment("panelUnassign", "fragmentUnassign", this);

                fragment.addOrReplace(
                        new IndicatingOnConfirmAjaxLink<Void>("unassignLink", "confirmUnassign", enabled) {

                    private static final long serialVersionUID = -6957616042924610294L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case ASSIGN:
                fragment = new Fragment("panelAssign", "fragmentAssign", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("assignLink") {

                    private static final long serialVersionUID = -6957616042924610304L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case DEPROVISION:
                fragment = new Fragment("panelDeprovision", "fragmentDeprovision", this);

                fragment.addOrReplace(
                        new IndicatingOnConfirmAjaxLink<Void>("deprovisionLink", "confirmDeprovision", enabled) {

                    private static final long serialVersionUID = -6957616042924610295L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case PROVISION:
                fragment = new Fragment("panelProvision", "fragmentProvision", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("provisionLink") {

                    private static final long serialVersionUID = -1876519166660008562L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case PROPAGATION_TASKS:
                fragment = new Fragment("panelPropagationTasks", "fragmentPropagationTasks", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("propagationTasksLink") {

                    private static final long serialVersionUID = -1876519166660008562L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case ZOOM_IN:
                fragment = new Fragment("panelZoomIn", "fragmentZoomIn", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("zoomInLink") {

                    private static final long serialVersionUID = -6957616042924610305L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            case ZOOM_OUT:
                fragment = new Fragment("panelZoomOut", "fragmentZoomOut", this);

                fragment.addOrReplace(new IndicatingAjaxLink<Void>("zoomOutLink") {

                    private static final long serialVersionUID = -6957616042924610305L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        link.onClick(target, model.getObject());
                    }

                    @Override
                    public String getAjaxIndicatorMarkupId() {
                        return disableIndicator ? StringUtils.EMPTY : super.getAjaxIndicatorMarkupId();
                    }
                }.setVisible(link.isEnabled(model.getObject())));
                break;

            default:
            // do nothing
        }

        if (fragment != null) {
            fragment.setEnabled(enabled);
            if (StringUtils.isNotBlank(entitlements)) {
                MetaDataRoleAuthorizationStrategy.authorize(fragment, ENABLE, entitlements);
            }
            super.addOrReplace(fragment);
        }

        return this;
    }

    public void remove(final ActionLink.ActionType type) {
        switch (type) {
            case CLAIM:
                super.addOrReplace(new Fragment("panelClaim", "emptyFragment", this));
                break;

            case MANAGE_RESOURCES:
                super.addOrReplace(new Fragment("panelManageResources", "emptyFragment", this));
                break;

            case MANAGE_USERS:
                super.addOrReplace(new Fragment("panelManageUsers", "emptyFragment", this));
                break;

            case MANAGE_GROUPS:
                super.addOrReplace(new Fragment("panelManageGroups", "emptyFragment", this));
                break;

            case MAPPING:
                super.addOrReplace(new Fragment("panelMapping", "emptyFragment", this));
                break;

            case MUSTCHANGEPASSWORD:
                super.addOrReplace(new Fragment("panelMustChangePassword", "emptyFragment", this));
                break;

            case RESET_TIME:
                super.addOrReplace(new Fragment("panelResetTime", "emptyFragment", this));
                break;

            case CLONE:
                super.addOrReplace(new Fragment("panelClone", "emptyFragment", this));
                break;

            case CREATE:
                super.addOrReplace(new Fragment("panelCreate", "emptyFragment", this));
                break;

            case EDIT:
                super.addOrReplace(new Fragment("panelEdit", "emptyFragment", this));
                break;

            case HTML_EDIT:
                super.addOrReplace(new Fragment("panelHtmlEdit", "emptyFragment", this));
                break;

            case TEXT_EDIT:
                super.addOrReplace(new Fragment("panelTestEdit", "emptyFragment", this));
                break;

            case VIEW:
                super.addOrReplace(new Fragment("panelView", "emptyFragment", this));
                break;

            case SEARCH:
                super.addOrReplace(new Fragment("panelSearch", "emptyFragment", this));
                break;

            case EXECUTE:
                super.addOrReplace(new Fragment("panelExecute", "emptyFragment", this));
                break;

            case DRYRUN:
                super.addOrReplace(new Fragment("panelDryRun", "emptyFragment", this));
                break;

            case DELETE:
                super.addOrReplace(new Fragment("panelDelete", "emptyFragment", this));
                break;

            case SELECT:
                super.addOrReplace(new Fragment("panelSelect", "emptyFragment", this));
                break;

            case CLOSE:
                super.addOrReplace(new Fragment("panelClose", "emptyFragment", this));
                break;

            case EXPORT:
                super.addOrReplace(new Fragment("panelExport", "emptyFragment", this));
                break;

            case SUSPEND:
                super.addOrReplace(new Fragment("panelSuspend", "emptyFragment", this));
                break;

            case REACTIVATE:
                super.addOrReplace(new Fragment("panelReactivate", "emptyFragment", this));
                break;

            case RELOAD:
                super.addOrReplace(new Fragment("panelReload", "emptyFragment", this));
                break;

            case CHANGE_VIEW:
                super.addOrReplace(new Fragment("panelChangeView", "emptyFragment", this));
                break;

            case UNLINK:
                super.addOrReplace(new Fragment("panelUnlink", "emptyFragment", this));
                break;

            case LINK:
                super.addOrReplace(new Fragment("panelLink", "emptyFragment", this));
                break;

            case UNASSIGN:
                super.addOrReplace(new Fragment("panelUnassign", "emptyFragment", this));
                break;

            case ASSIGN:
                super.addOrReplace(new Fragment("panelAssign", "emptyFragment", this));
                break;

            case DEPROVISION:
                super.addOrReplace(new Fragment("panelDeprovision", "emptyFragment", this));
                break;

            case PROVISION:
                super.addOrReplace(new Fragment("panelProvision", "emptyFragment", this));
                break;

            case ZOOM_IN:
                super.addOrReplace(new Fragment("panelZoomIn", "emptyFragment", this));
                break;

            case ZOOM_OUT:
                super.addOrReplace(new Fragment("panelZoomOut", "emptyFragment", this));
                break;

            default:
            // do nothing
        }
    }

    private ActionLinksPanel<T> setDisableIndicator(final boolean disableIndicator) {
        this.disableIndicator = disableIndicator;
        return this;
    }

    public static <T extends Serializable> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * ActionLinksPanel builder.
     *
     * @param <T> model object type.
     */
    public static final class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Map<ActionLink.ActionType, Triple<ActionLink<T>, String, Boolean>> actions = new HashMap<>();

        private boolean disableIndicator = false;

        public Builder<T> setDisableIndicator(final boolean disableIndicator) {
            this.disableIndicator = disableIndicator;
            return this;
        }

        public Builder<T> add(final ActionLink<T> link, final ActionLink.ActionType type) {
            return addWithRoles(link, type, null, true);
        }

        public Builder<T> add(
                final ActionLink<T> link,
                final ActionLink.ActionType type,
                final String entitlements) {

            return addWithRoles(link, type, entitlements, true);
        }

        public Builder<T> add(
                final ActionLink<T> link,
                final ActionLink.ActionType type,
                final String entitlement,
                final boolean enabled) {

            return addWithRoles(link, type, entitlement, enabled);
        }

        public Builder<T> addWithRoles(
                final ActionLink<T> link,
                final ActionLink.ActionType type,
                final String entitlements) {

            return addWithRoles(link, type, entitlements, true);
        }

        public Builder<T> addWithRoles(
                final ActionLink<T> link,
                final ActionLink.ActionType type,
                final String entitlements,
                final boolean enabled) {
            actions.put(type, Triple.of(link, entitlements, enabled));
            return this;
        }

        /**
         * Use this method to build an ation panel without any model reference.
         *
         * @param id Component id.
         * @return Action link panel.
         */
        public ActionLinksPanel<T> build(final String id) {
            return build(id, null);
        }

        /**
         * Use this methos to build an action panel including a model reference.
         *
         * @param id Component id.
         * @param modelObject model object.
         * @return Action link panel.
         */
        public ActionLinksPanel<T> build(final String id, final T modelObject) {
            final ActionLinksPanel<T> panel = modelObject == null
                    ? new ActionLinksPanel<>(id, new Model<T>())
                    : new ActionLinksPanel<>(id, new Model<>(modelObject));

            panel.setDisableIndicator(disableIndicator);

            for (Entry<ActionLink.ActionType, Triple<ActionLink<T>, String, Boolean>> action : actions.entrySet()) {
                panel.add(
                        action.getValue().getLeft(),
                        action.getKey(),
                        action.getValue().getMiddle(),
                        action.getValue().getRight());
            }
            return panel;
        }
    }
}
