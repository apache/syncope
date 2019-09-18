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
package org.apache.syncope.ide.netbeans;

public final class ConnectionParams {

    private String url;

    private String username;

    private String password;

    public static final class Builder {

        private String scheme;

        private String host;

        private String port;

        private String username;

        private String password;

        private Builder() {
        }

        public Builder scheme(final String value) {
            this.scheme = value;
            return this;
        }

        public Builder host(final String value) {
            this.host = value;
            return this;
        }

        public Builder port(final String value) {
            this.port = value;
            return this;
        }

        public Builder username(final String value) {
            this.username = value;
            return this;
        }

        public Builder password(final String value) {
            this.password = value;
            return this;
        }

        public ConnectionParams build() {
            return new ConnectionParams(scheme + "://" + host + ':' + port + "/syncope/rest", username, password);
        }
    }

    public static ConnectionParams.Builder builder() {
        return new ConnectionParams.Builder();
    }

    private ConnectionParams(
            final String url,
            final String userName,
            final String password) {
        this.url = url;
        this.username = userName;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public ConnectionParams port(final String value) {
        this.url = value;
        return this;
    }

    public ConnectionParams userName(final String value) {
        this.username = value;
        return this;
    }

    public ConnectionParams password(final String value) {
        this.password = value;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder;
        toStringBuilder = new StringBuilder("ConnectionParams{");
        toStringBuilder.append("url=").append(this.url);
        toStringBuilder.append(",username=").append(this.username);
        toStringBuilder.append(",password=").append(this.password);
        toStringBuilder.append('}');
        return toStringBuilder.toString();
    }

}
