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

import java.util.concurrent.TimeUnit;
import org.apache.syncope.client.console.wicket.ws.RefreshWebSocketBehavior;
import org.apache.wicket.PageReference;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;

public abstract class ExtAlertWidget extends AlertWidget {

    private static final long serialVersionUID = -5622060468533516192L;

    protected final PageReference pageRef;

    public ExtAlertWidget(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        add(new RefreshWebSocketBehavior() {

            private static final long serialVersionUID = -7095269057058900157L;

            @Override
            protected void onTimer(final WebSocketRequestHandler handler) {
                long latestAlterts = getLatestAlertsSize();
                if (!String.valueOf(latestAlterts).equals(linkAlertsNumber.getDefaultModelObjectAsString())) {
                    linkAlertsNumber.setDefaultModelObject(latestAlterts);
                    handler.add(linkAlertsNumber);

                    headerAlertsNumber.setDefaultModelObject(latestAlterts);
                    handler.add(headerAlertsNumber);
                }
            }
        }.schedule(30, TimeUnit.SECONDS));
    }
}
