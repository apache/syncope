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

import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;

public class AuditDataBinderImpl implements AuditDataBinder {

    @Override
    public AuditConfTO getAuditConfTO(final AuditConf auditConf) {
        AuditConfTO auditConfTO = new AuditConfTO();
        auditConfTO.setKey(auditConf.getKey());
        auditConfTO.setActive(auditConf.isActive());
        return auditConfTO;
    }
}
