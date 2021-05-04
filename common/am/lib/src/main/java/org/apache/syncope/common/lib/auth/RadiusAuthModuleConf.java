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
package org.apache.syncope.common.lib.auth;

public class RadiusAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -2235771400318503131L;

    /**
     * Radius protocol to use when communicating with the server.
     */
    private String protocol = "EAP_MSCHAPv2";

    private String inetAddress;

    private String sharedSecret;

    private int socketTimeout;

    private int authenticationPort = 1812;

    private int accountingPort = 1813;

    private int retries = 3;

    private String nasIdentifier;

    private long nasPort = -1;

    private long nasPortId = -1;

    private long nasRealPort = -1;

    private int nasPortType = -1;

    private String nasIpAddress;

    private String nasIpv6Address;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(final int retries) {
        this.retries = retries;
    }

    public String getNasIdentifier() {
        return nasIdentifier;
    }

    public void setNasIdentifier(final String nasIdentifier) {
        this.nasIdentifier = nasIdentifier;
    }

    public long getNasPort() {
        return nasPort;
    }

    public void setNasPort(final long nasPort) {
        this.nasPort = nasPort;
    }

    public long getNasPortId() {
        return nasPortId;
    }

    public void setNasPortId(final long nasPortId) {
        this.nasPortId = nasPortId;
    }

    public long getNasRealPort() {
        return nasRealPort;
    }

    public void setNasRealPort(final long nasRealPort) {
        this.nasRealPort = nasRealPort;
    }

    public int getNasPortType() {
        return nasPortType;
    }

    public void setNasPortType(final int nasPortType) {
        this.nasPortType = nasPortType;
    }

    public String getNasIpAddress() {
        return nasIpAddress;
    }

    public void setNasIpAddress(final String nasIpAddress) {
        this.nasIpAddress = nasIpAddress;
    }

    public String getNasIpv6Address() {
        return nasIpv6Address;
    }

    public void setNasIpv6Address(final String nasIpv6Address) {
        this.nasIpv6Address = nasIpv6Address;
    }

    public String getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(final String inetAddress) {
        this.inetAddress = inetAddress;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(final String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(final int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getAuthenticationPort() {
        return authenticationPort;
    }

    public void setAuthenticationPort(final int authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    public int getAccountingPort() {
        return accountingPort;
    }

    public void setAccountingPort(final int accountingPort) {
        this.accountingPort = accountingPort;
    }
}
