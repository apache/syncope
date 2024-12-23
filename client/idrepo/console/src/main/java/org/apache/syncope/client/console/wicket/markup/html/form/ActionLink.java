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
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class ActionLink<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 7031329706998320639L;

    private boolean reloadFeedbackPanel = true;

    private T modelObject;

    private boolean enabled = true;

    private String confirmMessage;

    public ActionLink() {
    }

    public ActionLink(final T modelObject) {
        this.modelObject = modelObject;
    }

    public enum ActionType {

        UP("up"),
        DOWN("down"),
        MAPPING("update"),
        MUSTCHANGEPASSWORD("update"),
        SET_LATEST_SYNC_TOKEN("update"),
        REMOVE_SYNC_TOKEN("update"),
        CLONE("create"),
        CREATE("create"),
        CREATE_CONNECTOR("create"),
        CREATE_RESOURCE("create"),
        TEMPLATE("read"),
        EDIT("read"),
        TYPE_EXTENSIONS("read"),
        HTML("read"),
        TEXT("read"),
        COMPOSE("update"),
        LAYOUT_EDIT("read"),
        RESET("update"),
        ENABLE("update"),
        NOT_FOUND("read"),
        VIEW("view"),
        MEMBERS("members"),
        SEARCH("search"),
        DELETE("delete"),
        EXECUTE("execute"),
        PASSWORD_MANAGEMENT("update"),
        REQUEST_PASSWORD_RESET("update"),
        DRYRUN("execute"),
        CLAIM("claim"),
        UNCLAIM("unclaim"),
        SELECT("read"),
        CLOSE("read"),
        EXPORT("read"),
        SUSPEND("update"),
        REACTIVATE("update"),
        RELOAD("import"),
        CHANGE_VIEW("changeView"),
        UNLINK("update"),
        LINK("update"),
        UNASSIGN("update"),
        ASSIGN("update"),
        DEPROVISION("update"),
        PROVISION("update"),
        DEPROVISION_MEMBERS("update"),
        PROVISION_MEMBERS("update"),
        RECONCILIATION_PUSH("update"),
        RECONCILIATION_PULL("update"),
        RECONCILIATION_RESOURCE("update"),
        MANAGE_RESOURCES("update"),
        MANAGE_ACCOUNTS("update"),
        MERGE_ACCOUNTS("update"),
        MANAGE_USERS("update"),
        MANAGE_GROUPS("update"),
        PROPAGATION_TASKS("read"),
        NOTIFICATION_TASKS("read"),
        PULL_TASKS("read"),
        LIVE_SYNC_TASK("read"),
        PUSH_TASKS("read"),
        ZOOM_IN("zoomin"),
        ZOOM_OUT("zoomout"),
        VIEW_EXECUTIONS("read"),
        VIEW_DETAILS("read"),
        MANAGE_APPROVAL("edit"),
        EDIT_APPROVAL("edit"),
        VIEW_AUDIT_HISTORY("read"),
        EXTERNAL_EDITOR("externalEditor"),
        EXPLORE_RESOURCE("search");

        private final String actionId;

        ActionType(final String actionId) {
            this.actionId = actionId;
        }

        public String getActionId() {
            return actionId;
        }
    }

    public T getModelObject() {
        return modelObject;
    }

    public abstract void onClick(AjaxRequestTarget target, T modelObject);

    public void postClick() {
    }

    public boolean feedbackPanelAutomaticReload() {
        return reloadFeedbackPanel;
    }

    public ActionLink<T> feedbackPanelAutomaticReload(final boolean reloadFeedbackPanel) {
        this.reloadFeedbackPanel = reloadFeedbackPanel;
        return this;
    }

    protected boolean statusCondition(final T modelObject) {
        return true;
    }

    public Class<? extends Page> getPageClass() {
        return null;
    }

    public PageParameters getPageParameters() {
        return null;
    }

    public final ActionLink<T> disable() {
        this.enabled = false;
        return this;
    }

    public final boolean isEnabled(final T modelObject) {
        return this.enabled && statusCondition(modelObject);
    }

    public boolean isIndicatorEnabled() {
        return true;
    }

    public String getConfirmMessage() {
        return confirmMessage;
    }

    public ActionLink<T> confirmMessage(final String confirmMessage) {
        this.confirmMessage = confirmMessage;
        return this;
    }
}
