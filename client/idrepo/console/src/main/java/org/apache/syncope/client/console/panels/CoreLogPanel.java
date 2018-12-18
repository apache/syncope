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

import org.apache.syncope.client.console.pages.LogViewer;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.PopupSettings;

public class CoreLogPanel extends AbstractLogsPanel<LoggerTO> {

    private static final long serialVersionUID = 3905038169553185171L;

    private final LoggerRestClient restClient = new LoggerRestClient();

    public CoreLogPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, new LoggerRestClient().listLogs());

        BookmarkablePageLink<Void> viewer = new BookmarkablePageLink<>("viewer", LogViewer.class);
        viewer.setPopupSettings(new PopupSettings().setHeight(600).setWidth(800));
        loggerContainer.add(viewer);
    }

    @Override
    protected void update(final LoggerTO loggerTO) {
        restClient.setLogLevel(loggerTO);
    }
}
