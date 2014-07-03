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
package org.apache.syncope.console.pages;

import java.util.List;
import org.apache.syncope.common.to.PushTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.UnmatchingRule;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.RoleSearchPanel;
import org.apache.syncope.console.pages.panels.UserSearchPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;

/**
 * Modal window with Push Task form.
 */
public class PushTaskModalPage extends AbstractSyncTaskModalPage {

    private static final long serialVersionUID = 2148403203517274669L;

    private final UserSearchPanel userFilter;

    private final RoleSearchPanel roleFilter;

    private final AjaxCheckBoxPanel checkUserFilter;

    private final AjaxCheckBoxPanel checkRoleFilter;

    @Override
    protected List<String> getSyncActions() {
        return taskRestClient.getPushActionsClasses();
    }

    public PushTaskModalPage(final ModalWindow window, final PushTaskTO taskTO, final PageReference pageRef) {

        super(window, taskTO, pageRef);

        // set default Matching rule
        ((DropDownChoice) matchingRule.getField()).setDefaultModelObject(taskTO.getMatchingRule() == null
                ? MatchingRule.UPDATE
                : taskTO.getMatchingRule());
        profile.add(matchingRule);

        // set default Unmatching rule
        ((DropDownChoice) unmatchingRule.getField()).setDefaultModelObject(taskTO.getUnmatchingRule() == null
                ? UnmatchingRule.ASSIGN
                : taskTO.getUnmatchingRule());
        profile.add(unmatchingRule);

        final WebMarkupContainer filterContainer = new WebMarkupContainer("filterContainer");
        filterContainer.setOutputMarkupId(true);

        checkUserFilter = new AjaxCheckBoxPanel("checkUserFilter", "checkUserFilter",
                new Model<Boolean>(taskTO.getUserFilter() != null));
        filterContainer.add(checkUserFilter);

        checkRoleFilter = new AjaxCheckBoxPanel("checkRoleFilter", "checkRoleFilter",
                new Model<Boolean>(taskTO.getRoleFilter() != null));
        filterContainer.add(checkRoleFilter);

        userFilter = new UserSearchPanel.Builder("userFilter").fiql(taskTO.getUserFilter()).build();
        userFilter.setEnabled(checkUserFilter.getModelObject());

        filterContainer.add(userFilter);

        roleFilter = new RoleSearchPanel.Builder("roleFilter").fiql(taskTO.getRoleFilter()).build();
        roleFilter.setEnabled(checkRoleFilter.getModelObject());
        filterContainer.add(roleFilter);

        checkUserFilter.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                userFilter.setEnabled(checkUserFilter.getModelObject());
                target.add(filterContainer);
            }
        });

        checkRoleFilter.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                roleFilter.setEnabled(checkRoleFilter.getModelObject());
                target.add(filterContainer);
            }
        });

        profile.add(filterContainer);
    }

    @Override
    public void submitAction(final SchedTaskTO taskTO) {
        setFilters((PushTaskTO) taskTO);
        if (taskTO.getId() > 0) {
            taskRestClient.updateSchedTask((PushTaskTO) taskTO);
        } else {
            taskRestClient.createSchedTask((PushTaskTO) taskTO);
        }
    }

    private void setFilters(final PushTaskTO pushTaskTO) {
        // set user filter if enabled
        pushTaskTO.setUserFilter(checkUserFilter.getModelObject() ? userFilter.buildFIQL() : null);
        // set role filter if enabled
        pushTaskTO.setRoleFilter(checkRoleFilter.getModelObject() ? roleFilter.buildFIQL() : null);
    }
}
