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
package org.apache.syncope.client.console.panels;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.form.CheckBoxMultipleChoiceFieldPanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Modal window with Connector form.
 */
public class ConnectorCapabilitiesPanel extends Panel {

    private static final long serialVersionUID = -2025535531121434050L;

    public ConnectorCapabilitiesPanel(final String id, final IModel<ConnInstanceTO> model) {

        super(id, model);
        setOutputMarkupId(true);

        final IModel<List<ConnectorCapability>> all = new LoadableDetachableModel<List<ConnectorCapability>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnectorCapability> load() {
                return Arrays.asList(ConnectorCapability.values());
            }
        };

        add(new CheckBoxMultipleChoiceFieldPanel<>(
                "capabilitiesPalette",
                new PropertyModel<List<ConnectorCapability>>(model.getObject(), "capabilities"),
                all));
    }
}
