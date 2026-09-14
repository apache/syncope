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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

public final class ZookeeperTestingServer {

    private static TestingServer ZK_SERVER;

    public static void start() throws Exception {
        if (ZK_SERVER == null) {
            Mutable<String> username = new MutableObject<>();
            Mutable<String> password = new MutableObject<>();
            try (InputStream propStream = ZookeeperServiceOpsTest.class.getResourceAsStream("/test.properties")) {
                Properties props = new Properties();
                props.load(propStream);

                username.setValue(props.getProperty("keymaster.username"));
                password.setValue(props.getProperty("keymaster.password"));
            } catch (Exception e) {
                fail("Could not load /test.properties", e);
            }

            System.setProperty(
                    "zookeeper.DigestAuthenticationProvider.superDigest",
                    DigestAuthenticationProvider.generateDigest(username.get() + ":" + password.get()));
            InstanceSpec spec = new InstanceSpec(null, 2181, -1, -1, true, 1, -1, -1, Map.of());
            ZK_SERVER = new TestingServer(spec, true);
        }
    }

    private ZookeeperTestingServer() {
        // private constructor for static utility class
    }
}
