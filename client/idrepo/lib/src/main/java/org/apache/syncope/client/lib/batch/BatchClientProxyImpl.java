/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.lib.batch;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;

public class BatchClientProxyImpl extends ClientProxyImpl {

    private final BatchClientFactoryBean factory;

    public BatchClientProxyImpl(
            final BatchClientFactoryBean factory,
            final URI baseURI,
            final ClassLoader loader,
            final ClassResourceInfo cri,
            final boolean isRoot,
            final boolean inheritHeaders,
            final Object... varValues) {

        super(baseURI, loader, cri, isRoot, inheritHeaders, varValues);
        this.factory = factory;
    }

    public BatchClientProxyImpl(
            final BatchClientFactoryBean factory,
            final ClientState initialState,
            final ClassLoader loader,
            final ClassResourceInfo cri,
            final boolean isRoot,
            final boolean inheritHeaders,
            final Object... varValues) {

        super(initialState, loader, cri, isRoot, inheritHeaders, varValues);
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object[] preProcessResult(final Message message) {
        BatchRequestItem bri = new BatchRequestItem();
        bri.setMethod((String) message.get(Message.HTTP_REQUEST_METHOD));
        bri.setRequestURI(StringUtils.substringAfter(
                (String) message.getContextualProperty(Message.REQUEST_URI),
                getState().getBaseURI().toASCIIString()));
        bri.setHeaders((Map<String, List<Object>>) message.get(Message.PROTOCOL_HEADERS));

        BatchOfflineHTTPConduit conduit = (BatchOfflineHTTPConduit) message.getExchange().getConduit(message);
        bri.setContent(conduit.getOutputStream().toString(StandardCharsets.UTF_8));

        factory.add(bri);
        return null;
    }

    @Override
    protected Object handleResponse(final Message outMessage, final Class<?> serviceCls) {
        return null;
    }
}
