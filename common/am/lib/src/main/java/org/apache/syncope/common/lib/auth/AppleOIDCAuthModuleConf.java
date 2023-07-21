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

public class AppleOIDCAuthModuleConf extends AbstractOIDCAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579522L;

    /**
     * Client secret expiration timeout.
     * This settings supports the java.time.Duration syntax.
     */
    protected String timeout = "PT30S";

    /**
     * Apple team identifier.
     * Usually, 10 character string given to you by Apple.
     */
    protected String teamId;

    /**
     * Private key obtained from Apple.
     * Must point to a resource that resolved to an elliptic curve (EC) private key.
     */
    protected String privateKey;

    /**
     * The identifier for the private key.
     * Usually the 10 character Key ID of the private key you create in Apple.
     */
    protected String privateKeyId;

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(final String timeout) {
        this.timeout = timeout;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public void setPrivateKeyId(final String privateKeyId) {
        this.privateKeyId = privateKeyId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(final String teamId) {
        this.teamId = teamId;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
