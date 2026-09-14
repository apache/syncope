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
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
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

        try {
            System.setProperty(
                    "zookeeper.DigestAuthenticationProvider.superDigest",
                    DigestAuthenticationProvider.generateDigest(username.get() + ":" + password.get()));
            InstanceSpec spec = new InstanceSpec(null, port.get(), -1, -1, true, 1, -1, -1, Map.of());
            new TestingServer(spec, true);
        } catch (Exception e) {
            fail(e);
        }
    }
}
