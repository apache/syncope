
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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(allOf = { PolicyTO.class })
public class AccessPolicyTO extends PolicyTO {

    private static final long serialVersionUID = -6711411162433533300L;

    private boolean enabled = true;

    private boolean ssoEnabled = true;

    private AccessPolicyConf conf;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", required = true, example = "org.apache.syncope.common.lib.policy.AccessPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSsoEnabled() {
        return this.ssoEnabled;
    }

    public void setSsoEnabled(final boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    public AccessPolicyConf getConf() {
        return conf;
    }

    public void setConf(final AccessPolicyConf conf) {
        this.conf = conf;
    }
}
