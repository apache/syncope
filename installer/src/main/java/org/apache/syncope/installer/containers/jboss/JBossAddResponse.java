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

public class JBossAddResponse {

    public class JBossBytesValue {

        private String bytesValue;

        public JBossBytesValue() {
            // default
        }

        public JBossBytesValue(final String bytesValue) {
            this.bytesValue = bytesValue;
        }

        public String getBytesValue() {
            return bytesValue;
        }

        public void setBytesValue(final String bytesValue) {
            this.bytesValue = bytesValue;
        }

    }

    private String outcome;

    private JBossBytesValue result;

    public JBossAddResponse() {
        // default
    }

    public JBossAddResponse(final String outcome, final JBossBytesValue result) {
        this.outcome = outcome;
        this.result = result;
    }

    public void setOutcome(final String outcome) {
        this.outcome = outcome;
    }

    public void setResult(final JBossBytesValue result) {
        this.result = result;
    }

    public String getOutcome() {
        return outcome;
    }

    public JBossBytesValue getResult() {
        return result;
    }

}
