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
package org.apache.syncope.client.console.wicket.protocol.ws.api;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WicketWebSocketJQueryResourceReference;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * Temporary work-around class for WICKET-6262.
 */
public abstract class SyncopeWebSocketBehavior extends WebSocketBehavior {

    private static final long serialVersionUID = -2137694236966234985L;

    private static final MetaDataKey<Object> IS_JAVA_SCRIPT_CONTRIBUTED = new MetaDataKey<Object>() {

        private static final long serialVersionUID = 3109256773218160485L;

    };

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        RequestCycle cycle = component.getRequestCycle();
        if (cycle.find(IPartialPageRequestHandler.class) == null) {
            Object contributed = cycle.getMetaData(IS_JAVA_SCRIPT_CONTRIBUTED);
            if (contributed == null) {
                cycle.setMetaData(IS_JAVA_SCRIPT_CONTRIBUTED, new Object());

                response.render(JavaScriptHeaderItem.forReference(WicketWebSocketJQueryResourceReference.get()));

                PackageTextTemplate webSocketSetupTemplate =
                        new PackageTextTemplate(WicketWebSocketJQueryResourceReference.class,
                                "res/js/wicket-websocket-setup.js.tmpl");

                Map<String, Object> variables = Generics.newHashMap();

                // set falsy JS values for the non-used parameter
                int pageId = component.getPage().getPageId();
                variables.put("pageId", pageId);
                variables.put("resourceName", "");

                WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(component.getApplication());

                CharSequence baseUrl = getBaseUrl(webSocketSettings);
                Args.notNull(baseUrl, "baseUrl");
                variables.put("baseUrl", baseUrl);

                CharSequence contextPath = getContextPath(webSocketSettings);
                Args.notNull(contextPath, "contextPath");
                variables.put("contextPath", contextPath);

                // preserve the application name for JSR356 based impl
                variables.put("applicationName", component.getApplication().getName());

                CharSequence filterPrefix = getFilterPrefix(webSocketSettings);
                Args.notNull(filterPrefix, "filterPrefix");
                variables.put("filterPrefix", filterPrefix);

                String webSocketSetupScript = webSocketSetupTemplate.asString(variables);

                response.render(OnDomReadyHeaderItem.forScript(webSocketSetupScript));
            }
        }
    }
}
