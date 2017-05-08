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
package org.apache.syncope.ext.elasticsearch.client;

import java.net.InetAddress;
import java.util.Map;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring {@link FactoryBean} for getting the Elasticsearch's {@link Client} singleton instance.
 */
public class ElasticsearchClientFactoryBean implements FactoryBean<Client>, DisposableBean {

    private Map<String, Object> settings;

    private Map<String, Integer> addresses;

    private Client client;

    public void setSettings(final Map<String, Object> settings) {
        this.settings = settings;
    }

    public void setAddresses(final Map<String, Integer> addresses) {
        this.addresses = addresses;
    }

    @Override
    public Client getObject() throws Exception {
        synchronized (this) {
            if (client == null) {
                PreBuiltTransportClient tClient = new PreBuiltTransportClient(Settings.builder().put(settings).build());

                for (Map.Entry<String, Integer> entry : addresses.entrySet()) {
                    tClient.addTransportAddress(
                            new InetSocketTransportAddress(InetAddress.getByName(entry.getKey()), entry.getValue()));
                }

                client = tClient;
            }
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return Client.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (client != null) {
            client.close();
        }
    }

}
