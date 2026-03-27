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
package org.apache.syncope.client.console.wicket.ws;

import com.giffing.wicket.spring.boot.starter.web.WicketWebInitializer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;

public class BasePageWebSocketBehavior extends WebSocketBehavior {

    private static final long serialVersionUID = -2572183403109560579L;

    protected abstract static class Child implements Serializable {

        private static final long serialVersionUID = -8242819397698846428L;

        protected final String nonce = UUID.randomUUID().toString();

    }

    public abstract static class OnTimerChild extends Child {

        private static final long serialVersionUID = -2368062250910082871L;

        protected final long period;

        protected final TimeUnit unit;

        public OnTimerChild(final long period, final TimeUnit unit) {
            this.period = period;
            this.unit = unit;
        }

        protected abstract void onTimer(WebSocketRequestHandler handler);
    }

    public abstract static class OnMessageChild extends Child {

        private static final long serialVersionUID = 2306119214592765956L;

        protected abstract void onMessage(WebSocketRequestHandler handler, TextMessage message);
    }

    protected record WSConnectionInfo(String sessionId, IKey ikey) implements Serializable {

    }

    protected record RefreshMessage(String nonce) implements IWebSocketPushMessage {

    }

    protected final List<Child> children = new ArrayList<>();

    protected WSConnectionInfo wsConnectionInfo;

    public void add(final Child child) {
        if (children.stream().noneMatch(c -> c.nonce.equals(child.nonce))) {
            children.add(child);

            if (child instanceof OnTimerChild onTimerChild && wsConnectionInfo != null) {
                schedule(onTimerChild, wsConnectionInfo.sessionId(), wsConnectionInfo.ikey());
            }
        }
    }

    protected void schedule(final OnTimerChild child, final String sessionId, final IKey ikey) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, Thread.ofVirtual().factory());
        executor.scheduleAtFixedRate(() -> {
            Application application = Application.get(WicketWebInitializer.WICKET_FILTERNAME);
            IWebSocketConnection wsConnection = WebSocketSettings.Holder.get(application).
                    getConnectionRegistry().getConnection(application, sessionId, ikey);
            Optional.ofNullable(wsConnection).filter(IWebSocketConnection::isOpen).ifPresentOrElse(
                    wsc -> wsc.sendMessage(new RefreshMessage(child.nonce)),
                    () -> executor.shutdownNow());
        }, 0, child.period, child.unit);
    }

    @Override
    protected void onConnect(final ConnectedMessage message) {
        wsConnectionInfo = new WSConnectionInfo(message.getSessionId(), message.getKey());

        children.stream().
                filter(OnTimerChild.class::isInstance).map(OnTimerChild.class::cast).
                forEach(child -> schedule(child, message.getSessionId(), message.getKey()));
    }

    @Override
    protected void onMessage(final WebSocketRequestHandler handler, final TextMessage message) {
        children.stream().
                filter(OnMessageChild.class::isInstance).map(OnMessageChild.class::cast).findFirst().
                ifPresent(child -> child.onMessage(handler, message));
    }

    @Override
    protected void onPush(final WebSocketRequestHandler handler, final IWebSocketPushMessage message) {
        if (message instanceof RefreshMessage refreshMessage) {
            children.stream().
                    filter(OnTimerChild.class::isInstance).map(OnTimerChild.class::cast).
                    filter(child -> child.nonce.equals(refreshMessage.nonce())).findFirst().
                    ifPresent(child -> child.onTimer(handler));
        }
    }

    @Override
    protected void onClose(final ClosedMessage message) {
        children.clear();
    }
}
