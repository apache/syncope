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
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class LinkedAccountPrivilegesPanel extends WizardStep {

    private static final long serialVersionUID = 3388483585148725922L;

    private final ApplicationRestClient applicationRestClient = new ApplicationRestClient();

    public LinkedAccountPrivilegesPanel(final LinkedAccountTO linkedAccountTO) {
        super();
        setOutputMarkupId(true);

        final LoadableDetachableModel<List<String>> availablePrivilges = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return applicationRestClient.list().stream().
                    flatMap(app -> app.getPrivileges().stream()).
                    map(PrivilegeTO::getKey).
                    distinct().
                    sorted().
                    collect(Collectors.toList());
            }
        };
        AjaxPalettePanel<String> privilegesPanel = new AjaxPalettePanel.Builder<String>().
                setAllowOrder(true).
                setAllowMoveAll(true).
                build("privileges",
                        new PropertyModel<>(linkedAccountTO, "privileges"),
                        new ListModel<>(availablePrivilges.getObject()));
        privilegesPanel.hideLabel();
        privilegesPanel.setOutputMarkupId(true);
        add(privilegesPanel);
    }

}
