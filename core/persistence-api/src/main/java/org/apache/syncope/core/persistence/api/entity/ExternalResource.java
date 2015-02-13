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
package org.apache.syncope.core.persistence.api.entity;

import org.apache.syncope.core.persistence.api.entity.user.UMapping;
import org.apache.syncope.core.persistence.api.entity.role.RMapping;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.identityconnectors.framework.common.objects.SyncToken;

public interface ExternalResource extends AnnotatedEntity<String> {

    AccountPolicy getAccountPolicy();

    PasswordPolicy getPasswordPolicy();

    SyncPolicy getSyncPolicy();

    Set<ConnConfProperty> getConnInstanceConfiguration();

    ConnInstance getConnector();

    TraceLevel getCreateTraceLevel();

    TraceLevel getUpdateTraceLevel();

    TraceLevel getDeleteTraceLevel();

    TraceLevel getSyncTraceLevel();

    List<String> getPropagationActionsClassNames();

    PropagationMode getPropagationMode();

    Integer getPropagationPriority();

    UMapping getUmapping();

    RMapping getRmapping();

    SyncToken getUsyncToken();

    String getSerializedUSyncToken();

    SyncToken getRsyncToken();

    String getSerializedRSyncToken();

    boolean isEnforceMandatoryCondition();

    boolean isPropagationPrimary();

    boolean isRandomPwdIfNotProvided();

    void setKey(String name);

    void setAccountPolicy(AccountPolicy accountPolicy);

    void setPasswordPolicy(PasswordPolicy passwordPolicy);

    void setSyncPolicy(SyncPolicy syncPolicy);

    void setConnInstanceConfiguration(Set<ConnConfProperty> properties);

    void setConnector(ConnInstance connector);

    void setCreateTraceLevel(TraceLevel createTraceLevel);

    void setUpdateTraceLevel(TraceLevel updateTraceLevel);

    void setDeleteTraceLevel(TraceLevel deleteTraceLevel);

    void setSyncTraceLevel(TraceLevel syncTraceLevel);

    void setPropagationMode(PropagationMode propagationMode);

    void setPropagationPriority(Integer priority);

    void setUmapping(UMapping umapping);

    void setRmapping(RMapping rmapping);

    void setEnforceMandatoryCondition(boolean enforce);

    void setPropagationPrimary(boolean condition);

    void setRandomPwdIfNotProvided(boolean condition);

    void setUsyncToken(SyncToken syncToken);

    void setRsyncToken(SyncToken syncToken);
}
