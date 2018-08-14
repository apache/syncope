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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;

public class BatchClientFactoryBean extends JAXRSClientFactoryBean {

    private final List<BatchRequestItem> batchRequestItems = new ArrayList<>();

    @Override
    protected ConduitSelector getConduitSelector(final Endpoint ep) {
        ConduitSelector cs = getConduitSelector();
        if (cs == null) {
            try {
                cs = new UpfrontConduitSelector(new BatchOfflineHTTPConduit(bus, ep.getEndpointInfo()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not create " + BatchOfflineHTTPConduit.class.getName(), e);
            }
        }
        cs.setEndpoint(ep);
        return cs;
    }

    @Override
    protected ClientProxyImpl createClientProxy(
            final ClassResourceInfo cri,
            final boolean isRoot,
            final ClientState actualState,
            final Object[] varValues) {

        if (actualState == null) {
            return new BatchClientProxyImpl(
                    this, URI.create(getAddress()), proxyLoader, cri, isRoot, inheritHeaders, varValues);
        } else {
            return new BatchClientProxyImpl(
                    this, actualState, proxyLoader, cri, isRoot, inheritHeaders, varValues);
        }
    }

    public boolean add(final BatchRequestItem item) {
        return this.batchRequestItems.add(item);
    }

    public List<BatchRequestItem> getBatchRequestItems() {
        return batchRequestItems;
    }
}
