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
package org.apache.syncope.client.console.widgets;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

public class ProgressesPanel extends Panel {

    private static final long serialVersionUID = 7837262802315339565L;

    public ProgressesPanel(final String id, final OffsetDateTime lastUpdate, final List<ProgressBean> progressBeans) {
        super(id);

        add(new Label("lastUpdate", SyncopeConsoleSession.get().getDateFormat().format(lastUpdate)));

        ListView<ProgressBean> progresses = new ListView<>("progresses", progressBeans) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<ProgressBean> item) {
                item.add(new ProgressPanel("progress", item.getModelObject()));
            }
        };
        progresses.setOutputMarkupId(true);
        add(progresses);
    }
}
