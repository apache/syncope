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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;

@XmlType
@XmlSeeAlso({ PullPolicyTO.class, PushPolicyTO.class })
@Schema(allOf = { PolicyTO.class })
public abstract class ProvisioningPolicyTO extends PolicyTO {

    private static final long serialVersionUID = -3786048942148269602L;

    private ConflictResolutionAction conflictResolutionAction;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    private final Map<String, String> correlationRules = new HashMap<>();

    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction == null
                ? ConflictResolutionAction.IGNORE
                : conflictResolutionAction;
    }

    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    @JsonProperty
    public Map<String, String> getCorrelationRules() {
        return correlationRules;
    }
}
