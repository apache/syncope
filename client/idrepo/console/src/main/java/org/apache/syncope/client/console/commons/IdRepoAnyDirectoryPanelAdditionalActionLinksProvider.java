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
package org.apache.syncope.client.console.commons;

import java.util.List;
import org.apache.syncope.client.console.panels.AnyObjectDirectoryPanel;
import org.apache.syncope.client.console.panels.GroupDirectoryPanel;
import org.apache.syncope.client.console.panels.UserDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.Action;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.IModel;

public class IdRepoAnyDirectoryPanelAdditionalActionLinksProvider
        implements AnyDirectoryPanelAdditionalActionLinksProvider {

    private static final long serialVersionUID = 121356001392126568L;

    @Override
    public List<Action<UserTO>> get(
            final IModel<UserTO> model,
            final String realm,
            final BaseModal<AnyWrapper<UserTO>> modal,
            final String header,
            final UserDirectoryPanel parentPanel,
            final PageReference pageRef) {

        return List.of();
    }

    @Override
    public List<Action<GroupTO>> get(
            final GroupTO modelObject,
            final String realm,
            final BaseModal<AnyWrapper<GroupTO>> modal,
            final String header,
            final GroupDirectoryPanel parentPanel,
            final PageReference pageRef) {

        return List.of();
    }

    @Override
    public List<Action<AnyObjectTO>> get(
            final String type,
            final AnyObjectTO modelObject,
            final String realm,
            final BaseModal<AnyWrapper<AnyObjectTO>> modal,
            final String header,
            final AnyObjectDirectoryPanel parentPanel,
            final PageReference pageRef) {

        return List.of();
    }
}
