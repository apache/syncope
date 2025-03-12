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
package org.apache.syncope.ext.openfga.client;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties("openfga")
public class OpenFGAProperties implements InitializingBean {

    private static void assertPositive(final Duration duration, final String fieldName) {
        if (duration != null && duration.isNegative()) {
            throw new IllegalStateException("%s must be positive".formatted(fieldName));
        }
    }

    /**
     * URL to OpenFGA instance.
     * If configured, beans will be initialized.
     */
    private String apiUrl;

    /**
     * API token for authenticating requests.
     * If null, anonymous requests will be sent out.
     */
    private String apiToken;

    /**
     * The read timeout for request.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration readTimeout = Duration.ofSeconds(10);

    /**
     * The connect timeout for requests.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectTimeout = Duration.ofSeconds(10);

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(final String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(final String apiToken) {
        this.apiToken = apiToken;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(final Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public void afterPropertiesSet() {
        assertPositive(readTimeout, "readTimeout");
        assertPositive(connectTimeout, "connectTimeout");
    }
}
