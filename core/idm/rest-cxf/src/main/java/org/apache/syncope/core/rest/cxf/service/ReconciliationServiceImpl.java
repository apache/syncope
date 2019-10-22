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
package org.apache.syncope.core.rest.cxf.service;

import javax.validation.ValidationException;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationServiceImpl extends AbstractServiceImpl implements ReconciliationService {

    @Autowired
    private ReconciliationLogic logic;

    private void validate(final ReconQuery reconQuery) {
        if ((reconQuery.getAnyKey() == null && reconQuery.getConnObjectKeyValue() == null)
                || (reconQuery.getAnyKey() != null && reconQuery.getConnObjectKeyValue() != null)) {

            throw new ValidationException("Either provide anyKey or connObjectKeyValue, not both");
        }
    }

    @Override
    public ReconStatus status(final ReconQuery reconQuery) {
        validate(reconQuery);
        return logic.status(reconQuery);
    }

    @Override
    public void push(final ReconQuery reconQuery, final PushTaskTO pushTask) {
        validate(reconQuery);
        logic.push(reconQuery, pushTask);
    }

    @Override
    public void pull(final ReconQuery reconQuery, final PullTaskTO pullTask) {
        validate(reconQuery);
        logic.pull(reconQuery, pullTask);
    }
}
