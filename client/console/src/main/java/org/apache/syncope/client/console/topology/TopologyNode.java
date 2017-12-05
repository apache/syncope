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
package org.apache.syncope.client.console.topology;

import java.io.Serializable;

public class TopologyNode implements Serializable {

    public enum Kind {

        RESOURCE,
        CONNECTOR,
        CONNECTOR_SERVER,
        FS_PATH,
        SYNCOPE

    }

    public enum Status {

        UNKNOWN,
        REACHABLE,
        UNREACHABLE,
        FAILURE

    }

    private static final long serialVersionUID = -1506421230369224142L;

    private final String key;

    private String displayName;

    private String connectionDisplayName;

    private Kind kind;

    private String host;

    private int port;

    private int x;

    private int y;

    public TopologyNode(final String key, final String displayName, final Kind kind) {
        this.key = key;
        this.displayName = displayName;
        this.kind = kind;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getConnectionDisplayName() {
        return connectionDisplayName;
    }

    public void setConnectionDisplayName(final String connectionDisplayName) {
        this.connectionDisplayName = connectionDisplayName;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
    }

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

}
