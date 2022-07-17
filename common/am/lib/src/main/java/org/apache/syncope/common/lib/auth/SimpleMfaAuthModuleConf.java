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

import java.util.Map;
import org.apache.syncope.common.lib.to.AuthModuleTO;

public class SimpleMfaAuthModuleConf implements MFAAuthModuleConf {

    private static final long serialVersionUID = -7663257599139312426L;

    private long timeToKillInSeconds = 30L;

    private int tokenLength = 6;

    private String bypassGroovyScript;

    private String emailAttribute = "email";

    private String emailFrom;

    private String emailSubject;

    private String emailText;

    @Override
    public String getFriendlyName() {
        return "CAS Simple Multifactor Authentication";
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(final String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(final String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailText() {
        return emailText;
    }

    public void setEmailText(final String emailText) {
        this.emailText = emailText;
    }

    public String getBypassGroovyScript() {
        return bypassGroovyScript;
    }

    public void setBypassGroovyScript(final String bypassGroovyScript) {
        this.bypassGroovyScript = bypassGroovyScript;
    }

    public String getEmailAttribute() {
        return emailAttribute;
    }

    public void setEmailAttribute(final String emailAttribute) {
        this.emailAttribute = emailAttribute;
    }

    public long getTimeToKillInSeconds() {
        return timeToKillInSeconds;
    }

    public void setTimeToKillInSeconds(final long timeToKillInSeconds) {
        this.timeToKillInSeconds = timeToKillInSeconds;
    }

    public int getTokenLength() {
        return tokenLength;
    }

    public void setTokenLength(final int tokenLength) {
        this.tokenLength = tokenLength;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
