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
package org.apache.syncope.core.persistence.api.entity.resource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.ProvidedKeyEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.identityconnectors.framework.common.objects.ObjectClass;

public interface ExternalResource extends ProvidedKeyEntity {

    ConnInstance getConnector();

    void setConnector(ConnInstance connector);

    Set<ConnConfProperty> getConfOverride();

    void setConfOverride(Set<ConnConfProperty> confOverride);

    boolean isOverrideCapabilities();

    void setOverrideCapabilities(boolean overrideCapabilities);

    Set<ConnectorCapability> getCapabilitiesOverride();

    AccountPolicy getAccountPolicy();

    void setAccountPolicy(AccountPolicy accountPolicy);

    PasswordPolicy getPasswordPolicy();

    void setPasswordPolicy(PasswordPolicy passwordPolicy);

    PullPolicy getPullPolicy();

    void setPullPolicy(PullPolicy pullPolicy);

    PushPolicy getPushPolicy();

    Implementation getProvisionSorter();

    void setProvisionSorter(Implementation provisionSorter);

    void setPushPolicy(PushPolicy pushPolicy);

    TraceLevel getCreateTraceLevel();

    void setCreateTraceLevel(TraceLevel createTraceLevel);

    TraceLevel getUpdateTraceLevel();

    void setUpdateTraceLevel(TraceLevel updateTraceLevel);

    TraceLevel getDeleteTraceLevel();

    void setDeleteTraceLevel(TraceLevel deleteTraceLevel);

    TraceLevel getProvisioningTraceLevel();

    void setProvisioningTraceLevel(TraceLevel provisioningTraceLevel);

    boolean add(Implementation propagationAction);

    List<? extends Implementation> getPropagationActions();

    Integer getPropagationPriority();

    void setPropagationPriority(Integer priority);

    boolean isEnforceMandatoryCondition();

    void setEnforceMandatoryCondition(boolean enforce);

    boolean isRandomPwdIfNotProvided();

    void setRandomPwdIfNotProvided(boolean condition);

    boolean add(Provision provision);

    Optional<? extends Provision> getProvision(String anyType);

    Optional<? extends Provision> getProvision(AnyType anyType);

    Optional<? extends Provision> getProvision(ObjectClass objectClass);

    List<? extends Provision> getProvisions();

    OrgUnit getOrgUnit();

    void setOrgUnit(OrgUnit orgUnit);
}
