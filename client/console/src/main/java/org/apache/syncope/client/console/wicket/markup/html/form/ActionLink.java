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
import org.apache.wicket.ajax.AjaxRequestTarget;

public abstract class ActionLink<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 7031329706998320639L;

    private boolean reloadFeedbackPanel = true;

    private T modelObject;

    private boolean enabled = true;

    public ActionLink() {
    }

    public ActionLink(final T modelObject) {
        this.modelObject = modelObject;
    }

    public enum ActionType {

        MAPPING("update"),
        MUSTCHANGEPASSWORD("update"),
        SET_LATEST_SYNC_TOKEN("update"),
        REMOVE_SYNC_TOKEN("update"),
        CLONE("create"),
        CREATE("create"),
        TEMPLATE("read"),
        EDIT("read"),
        TYPE_EXTENSIONS("read"),
        FO_EDIT("read"),
        HTML("read"),
        TEXT("read"),
        COMPOSE("update"),
        LAYOUT_EDIT("read"),
        RESET("update"),
        ENABLE("update"),
        NOT_FOND("read"),
        VIEW("view"),
        MEMBERS("members"),
        SEARCH("search"),
        DELETE("delete"),
        EXECUTE("execute"),
        PASSWORD_RESET("update"),
        DRYRUN("execute"),
        CLAIM("claim"),
        SELECT("read"),
        CLOSE("read"),
        EXPORT("read"),
        EXPORT_CSV("read"),
        EXPORT_HTML("read"),
        EXPORT_PDF("read"),
        EXPORT_RTF("read"),
        EXPORT_XML("read"),
        SUSPEND("update"),
        REACTIVATE("update"),
        RELOAD("reload"),
        CHANGE_VIEW("changeView"),
        UNLINK("update"),
        LINK("update"),
        UNASSIGN("update"),
        ASSIGN("update"),
        DEPROVISION("update"),
        PROVISION("update"),
        DEPROVISION_MEMBERS("update"),
        PROVISION_MEMBERS("update"),
        MANAGE_RESOURCES("update"),
        MANAGE_USERS("update"),
        MANAGE_GROUPS("update"),
        PROPAGATION_TASKS("read"),
        NOTIFICATION_TASKS("read"),
        ZOOM_IN("zoomin"),
        ZOOM_OUT("zoomout");

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

    public abstract void onClick(final AjaxRequestTarget target, final T modelObject);

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
}
