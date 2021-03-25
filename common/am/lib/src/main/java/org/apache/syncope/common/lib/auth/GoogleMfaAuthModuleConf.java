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

public class GoogleMfaAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -7883257599139312426L;

    /**
     * Issuer used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String issuer = "Syncope";

    /**
     * Label used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String label = "Syncope";

    /**
     * Length of the generated code.
     */
    private int codeDigits = 6;

    /**
     * The expiration time of the generated code in seconds.
     */
    private long timeStepSize = 30;

    /**
     * Since TOTP passwords are time-based, it is essential that
     * the clock of both the server and
     * the client are synchronised within
     * the tolerance defined here as the window size.
     */
    private int windowSize = 3;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public int getCodeDigits() {
        return codeDigits;
    }

    public void setCodeDigits(final int codeDigits) {
        this.codeDigits = codeDigits;
    }

    public long getTimeStepSize() {
        return timeStepSize;
    }

    public void setTimeStepSize(final long timeStepSize) {
        this.timeStepSize = timeStepSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }
}
