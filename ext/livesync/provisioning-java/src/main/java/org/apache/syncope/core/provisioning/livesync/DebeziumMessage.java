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
package org.apache.syncope.core.provisioning.livesync;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

public class DebeziumMessage implements Serializable {
    
    private Map<String, Object> before;

    private Map<String, Object> after;

    private Map<String, Object> source;

    private String op;
    @JsonProperty("ts_ms")
    private Double timestamp;

    private String transaction;

    public Map<String, Object> getBefore() {
        return before;
    }

    public void setBefore(final Map<String, Object> before) {
        this.before = before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

    public void setAfter(final Map<String, Object> after) {
        this.after = after;
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public void setSource(final Map<String, Object> source) {
        this.source = source;
    }

    public String getOp() {
        return op;
    }

    public void setOp(final String op) {
        this.op = op;
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Double timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(final String transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        return "DebeziumMessage{"
                + "before=" + before
                + ", after=" + after
                + ", source=" + source
                + ", op=" + op
                + ", timestamp=" + timestamp
                + ", transaction=" + transaction
                + '}';
    }
}
