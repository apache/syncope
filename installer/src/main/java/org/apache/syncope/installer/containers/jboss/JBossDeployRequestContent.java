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
package org.apache.syncope.installer.containers.jboss;

public class JBossDeployRequestContent {

    private final JBossDeployRequest[] content = new JBossDeployRequest[1];

    private final JBossDeployment[] address = new JBossDeployment[1];

    private final String operation = "add";

    private final String enabled = "true";

    public JBossDeployRequestContent(final String hash, final String address) {
        this.content[0] = new JBossDeployRequest(hash);
        this.address[0] = new JBossDeployment(address);
    }

    public class JBossDeployRequest {

        private JBossBytesValue hash;

        public JBossBytesValue getHash() {
            return hash;
        }

        public JBossDeployRequest(final String hash) {
            this.hash = new JBossBytesValue(hash);
        }
    }

    public class JBossBytesValue {

        private String bytesValue;

        public JBossBytesValue(final String bytesValue) {
            this.bytesValue = bytesValue;
        }
    }

    public class JBossDeployment {

        private String deployment;

        public JBossDeployment(final String deployment) {
            this.deployment = deployment;
        }

        public String getDeployment() {
            return deployment;
        }
    }
}
