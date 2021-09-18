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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;

public class RemediationDataBinderImpl implements RemediationDataBinder {

    @Override
    public RemediationTO getRemediationTO(final Remediation remediation) {
        RemediationTO remediationTO = new RemediationTO();
        remediationTO.setKey(remediation.getKey());
        remediationTO.setOperation(remediation.getOperation());
        remediationTO.setError(remediation.getError());
        remediationTO.setInstant(remediation.getInstant());
        remediationTO.setRemoteName(remediation.getRemoteName());

        switch (remediation.getOperation()) {
            case CREATE:
                remediationTO.setAnyCRPayload(
                        remediation.getPayloadAsCR(remediation.getAnyType().getKind().getCRClass()));
                break;

            case UPDATE:
                remediationTO.setAnyURPayload(
                        remediation.getPayloadAsUR(remediation.getAnyType().getKind().getURClass()));
                break;

            case DELETE:
                remediationTO.setKeyPayload(remediation.getPayloadAsKey());
                break;

            default:
        }

        remediationTO.setAnyType(remediation.getAnyType().getKey());

        if (remediation.getPullTask() != null) {
            remediationTO.setPullTask(remediation.getPullTask().getKey());
            remediationTO.setResource(remediation.getPullTask().getResource().getKey());
        }

        return remediationTO;
    }
}
