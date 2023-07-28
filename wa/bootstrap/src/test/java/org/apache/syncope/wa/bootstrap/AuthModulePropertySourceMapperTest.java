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
package org.apache.syncope.wa.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.apache.syncope.common.lib.auth.OAuth20AuthModuleConf;
import org.apache.syncope.common.lib.auth.SimpleMfaAuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.wa.bootstrap.mapping.AuthModulePropertySourceMapper;
import org.junit.jupiter.api.Test;

public class AuthModulePropertySourceMapperTest {

    @Test
    public void mapSimpleMfaAuthModuleConf() {
        AuthModuleTO authModuleTO = new AuthModuleTO();
        authModuleTO.setKey("key");
        authModuleTO.setOrder(0);

        SimpleMfaAuthModuleConf conf = new SimpleMfaAuthModuleConf();

        conf.setEmailAttribute("email");
        conf.setEmailFrom("syncope@apache.org");
        conf.setEmailSubject("Subject");
        conf.setEmailText("Text body");

        conf.setTokenLength(256);
        conf.setTimeToKillInSeconds(600);

        Map<String, Object> map = new AuthModulePropertySourceMapper(null).map(authModuleTO, conf);
        assertFalse(map.keySet().stream().anyMatch(k -> k.endsWith("defined")));
    }

    @Test
    public void mapOAuth20AuthModuleConf() {
        AuthModuleTO authModuleTO = new AuthModuleTO();
        authModuleTO.setKey("oauth20");
        authModuleTO.setOrder(0);

        OAuth20AuthModuleConf conf = new OAuth20AuthModuleConf();

        conf.setClientId("1000");
        conf.setClientSecret("secret");
        conf.setClientName("oauth20");
        conf.setEnabled(true);
        conf.setCustomParams(Map.of("param1", "param1"));
        conf.setAuthUrl("https://localhost/oauth2/auth");
        conf.setProfileUrl("https://localhost/oauth2/profile");
        conf.setTokenUrl("https://localhost/oauth2/token");
        conf.setResponseType("code");
        conf.setScope("cns");
        conf.setUserIdAttribute("uid");
        conf.setWithState(true);

        Map<String, Object> map = new AuthModulePropertySourceMapper(null).map(authModuleTO, conf);
        assertFalse(map.keySet().stream().anyMatch(k -> k.endsWith("defined")));
    }
}
