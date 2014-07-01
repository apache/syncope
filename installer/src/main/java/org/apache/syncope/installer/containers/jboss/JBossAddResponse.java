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

        public String BYTES_VALUE;

        public JBossBytesValue() {
        }

        public JBossBytesValue(String BYTES_VALUE) {
            this.BYTES_VALUE = BYTES_VALUE;
        }

        public String getBYTES_VALUE() {
            return BYTES_VALUE;
        }

        public void setBYTES_VALUE(String BYTES_VALUE) {
            this.BYTES_VALUE = BYTES_VALUE;
        }

    }

    public String outcome;

    public JBossBytesValue result;

    public JBossAddResponse() {
    }

    public JBossAddResponse(String outcome, JBossBytesValue result) {
        this.outcome = outcome;
        this.result = result;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setResult(JBossBytesValue result) {
        this.result = result;
    }

    public String getOutcome() {
        return outcome;
    }

    public JBossBytesValue getResult() {
        return result;
    }

}
