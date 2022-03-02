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
package org.apache.syncope.common.keymaster.client.zookeeper;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link ConfParamOps} via Apache Curator / Zookeeper.
 */
public class ZookeeperConfParamOps implements ConfParamOps {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfParamOps.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String CONF_PATH = "/conf";

    protected final CuratorFramework client;

    protected static String buildConfPath(final String... parts) {
        return CONF_PATH + '/' + String.join("/", parts);
    }

    public ZookeeperConfParamOps(final CuratorFramework client) {
        this.client = client;
    }

    @Override
    public Map<String, Object> list(final String domain) {
        try {
            if (client.checkExists().forPath(buildConfPath(domain)) == null) {
                client.create().creatingParentContainersIfNeeded().forPath(buildConfPath(domain));
            }

            Map<String, Object> list = new TreeMap<>();
            for (String child : client.getChildren().forPath(buildConfPath(domain))) {
                list.put(child, MAPPER.readValue(client.getData().forPath(buildConfPath(domain, child)), Object.class));
            }

            return list;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        T value = null;
        try {
            value = MAPPER.readValue(client.getData().forPath(buildConfPath(domain, key)), reference);
        } catch (KeeperException.NoNodeException e) {
            LOG.debug("Node {} was not found", buildConfPath(domain, key));
        } catch (Exception e) {
            throw new KeymasterException(e);
        }

        return Optional.ofNullable(value).orElse(defaultValue);
    }

    @Override
    public <T> void set(final String domain, final String key, final T value) {
        if (value == null) {
            remove(domain, key);
        } else {
            try {
                if (client.checkExists().forPath(buildConfPath(domain, key)) == null) {
                    client.create().creatingParentContainersIfNeeded().forPath(buildConfPath(domain, key));
                }

                client.setData().forPath(buildConfPath(domain, key), MAPPER.writeValueAsBytes(value));
            } catch (Exception e) {
                throw new KeymasterException(e);
            }
        }
    }

    @Override
    public void remove(final String domain, final String key) {
        try {
            client.delete().forPath(buildConfPath(domain, key));
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }
}
