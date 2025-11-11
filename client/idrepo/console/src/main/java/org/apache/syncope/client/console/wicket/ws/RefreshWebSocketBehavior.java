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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;

public abstract class RefreshWebSocketBehavior extends WebSocketBehavior {

    private record RefreshMessage(String nonce) implements IWebSocketPushMessage {

    }

    private static final long serialVersionUID = 5636572627689425575L;

    protected final Mutable<Pair<String, IKey>> websocketInfo = new MutableObject<>();

    protected final String nonce;

    public RefreshWebSocketBehavior() {
        nonce = UUID.randomUUID().toString();
    }

    @Override
    protected void onConnect(final ConnectedMessage message) {
        websocketInfo.setValue(Pair.of(message.getSessionId(), message.getKey()));
    }

    protected abstract void onTimer(WebSocketRequestHandler handler);

    @Override
    protected void onPush(final WebSocketRequestHandler handler, final IWebSocketPushMessage message) {
        if (message instanceof RefreshMessage refreshMessage && nonce.equals(refreshMessage.nonce())) {
            onTimer(handler);
        }
    }

    public RefreshWebSocketBehavior schedule(final long period, final TimeUnit unit) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, Thread.ofVirtual().factory());
        executor.scheduleAtFixedRate(() -> {
            if (websocketInfo.get() == null) {
                return;
            }

            Application application = Application.get(WicketWebInitializer.WICKET_FILTERNAME);
            IWebSocketConnection wsConnection = WebSocketSettings.Holder.get(application).getConnectionRegistry().
                    getConnection(application, websocketInfo.get().getLeft(), websocketInfo.get().getRight());
            Optional.ofNullable(wsConnection).filter(IWebSocketConnection::isOpen).ifPresentOrElse(
                    wsc -> wsc.sendMessage(new RefreshMessage(nonce)),
                    () -> executor.shutdownNow());
        }, 0, period, unit);

        return this;
    }
}
