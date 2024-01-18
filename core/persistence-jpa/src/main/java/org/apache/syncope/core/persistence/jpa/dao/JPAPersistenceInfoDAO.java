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
package org.apache.syncope.core.persistence.jpa.dao;

import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.event.TCPRemoteCommitProvider;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public class JPAPersistenceInfoDAO implements PersistenceInfoDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(PersistenceInfoDAO.class);

    protected final EntityManagerFactory entityManagerFactory;

    public JPAPersistenceInfoDAO(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();

        OpenJPAEntityManagerFactorySPI emfspi = entityManagerFactory.unwrap(OpenJPAEntityManagerFactorySPI.class);
        OpenJPAConfiguration conf = emfspi.getConfiguration();

        Map<String, Object> properties = emfspi.getProperties();
        result.put("vendor", properties.get("VendorName"));
        result.put("version", properties.get("VersionNumber"));
        result.put("platform", properties.get("Platform"));

        Map<String, Object> remoteCommitProvider = new LinkedHashMap<>();
        result.put("remoteCommitProvider", remoteCommitProvider);

        RemoteCommitEventManager rcem = conf.getRemoteCommitEventManager();

        remoteCommitProvider.put("remoteEventsEnabled", rcem.areRemoteEventsEnabled());
        remoteCommitProvider.put("transmitPersistedObjectIds", rcem.getTransmitPersistedObjectIds());
        remoteCommitProvider.put("failFast", rcem.isFailFast());

        RemoteCommitProvider rcp = rcem.getRemoteCommitProvider();
        List<Triple<String, Integer, Boolean>> addresses = new ArrayList<>();
        if (rcp instanceof TCPRemoteCommitProvider) {
            try {
                Field addressesField = ReflectionUtils.findField(TCPRemoteCommitProvider.class, "_addresses");
                addressesField.setAccessible(true);

                Class<?> hostClass = ClassUtils.forName(
                        "org.apache.openjpa.event.TCPRemoteCommitProvider$HostAddress",
                        ClassUtils.getDefaultClassLoader());
                Field addressField = ReflectionUtils.findField(hostClass, "_address");
                addressField.setAccessible(true);
                Field portField = ReflectionUtils.findField(hostClass, "_port");
                portField.setAccessible(true);
                Field isAvailableField = ReflectionUtils.findField(hostClass, "_isAvailable");
                isAvailableField.setAccessible(true);

                @SuppressWarnings("unchecked")
                List<Object> hosts = (List<Object>) ReflectionUtils.getField(addressesField, rcp);
                hosts.forEach(host -> {
                    InetAddress address = (InetAddress) ReflectionUtils.getField(addressField, host);
                    Integer port = (Integer) ReflectionUtils.getField(portField, host);
                    Boolean isAvailable = (Boolean) ReflectionUtils.getField(isAvailableField, host);

                    addresses.add(Triple.of(address.getHostAddress(), port, isAvailable));
                });
            } catch (Exception e) {
                LOG.error("Could not fetch information about TCPRemoteCommitProvider", e);
            }
        }

        remoteCommitProvider.put(
                "addresses",
                addresses.stream().map(address -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("ip", address.getLeft());
                    map.put("port", address.getMiddle());
                    map.put("available", address.getRight());
                    return map;
                }).toList());

        return result;
    }
}
