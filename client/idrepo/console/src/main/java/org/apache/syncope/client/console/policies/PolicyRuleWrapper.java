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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.types.ImplementationEngine;

public class PolicyRuleWrapper implements Serializable {

    private static final long serialVersionUID = 2472755929742424558L;

    private final boolean isNew;

    private String implementationKey;

    private ImplementationEngine implementationEngine;

    private RuleConf conf;

    public PolicyRuleWrapper(final boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getImplementationKey() {
        return implementationKey;
    }

    public PolicyRuleWrapper setImplementationKey(final String implementationKey) {
        this.implementationKey = implementationKey;
        return this;
    }

    public ImplementationEngine getImplementationEngine() {
        return implementationEngine;
    }

    public PolicyRuleWrapper setImplementationEngine(final ImplementationEngine implementationEngine) {
        this.implementationEngine = implementationEngine;
        return this;
    }

    public RuleConf getConf() {
        return conf;
    }

    public PolicyRuleWrapper setConf(final RuleConf conf) {
        this.conf = conf;
        return this;
    }
}
