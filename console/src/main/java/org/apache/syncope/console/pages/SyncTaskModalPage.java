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

import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.UnmatchingRule;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;

/**
 * Modal window with Sync Task form.
 */
public class SyncTaskModalPage extends AbstractSyncTaskModalPage {

    private static final long serialVersionUID = 2148403203517274669L;

    public SyncTaskModalPage(final ModalWindow window, final SyncTaskTO taskTO, final PageReference pageRef) {

        super(window, taskTO, pageRef);

        // set default Matching rule
        ((DropDownChoice) matchingRule.getField()).setDefaultModelObject(taskTO.getMatchingRule() == null
                ? MatchingRule.UPDATE
                : taskTO.getMatchingRule());
        profile.add(matchingRule);

        // set default Unmatching rule
        ((DropDownChoice) unmatchingRule.getField()).setDefaultModelObject(taskTO.getUnmatchingRule() == null
                ? UnmatchingRule.PROVISION
                : taskTO.getUnmatchingRule());
        profile.add(unmatchingRule);

        final AjaxCheckBoxPanel fullReconciliation = new AjaxCheckBoxPanel("fullReconciliation",
                getString("fullReconciliation"), new PropertyModel<Boolean>(taskTO, "fullReconciliation"));
        profile.add(fullReconciliation);
    }

    @Override
    public void submitAction(final SchedTaskTO taskTO) {
        if (taskTO.getId() > 0) {
            taskRestClient.updateSyncTask((SyncTaskTO) taskTO);
        } else {
            taskRestClient.createSyncTask((SyncTaskTO) taskTO);
        }
    }
}
