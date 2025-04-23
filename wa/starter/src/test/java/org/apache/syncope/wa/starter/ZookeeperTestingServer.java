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
package org.apache.syncope.wa.starter;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.server.auth.DigestLoginModule;
import org.apache.zookeeper.server.auth.SASLAuthenticationProvider;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ZookeeperTestingServer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(final ConfigurableApplicationContext ctx) {
        Mutable<Integer> port = new MutableObject<>();
        Mutable<String> username = new MutableObject<>();
        Mutable<String> password = new MutableObject<>();
        try (InputStream propStream = getClass().getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            port.setValue(Integer.valueOf(StringUtils.substringAfter(props.getProperty("keymaster.address"), ":")));
            username.setValue(props.getProperty("keymaster.username"));
            password.setValue(props.getProperty("keymaster.password"));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load /test.properties", e);
        }

        Configuration.setConfiguration(new Configuration() {

            private final AppConfigurationEntry[] entries = {
                new AppConfigurationEntry(
                DigestLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                Map.of("user_" + username.getValue(), password.getValue()))
            };

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
                return entries;
            }
        });

        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("authProvider.1", SASLAuthenticationProvider.class.getName());
        InstanceSpec spec = new InstanceSpec(null, port.getValue(), -1, -1, true, 1, -1, -1, customProperties);

        try {
            new TestingServer(spec, true);
        } catch (Exception e) {
            fail(e);
        }
    }
}
