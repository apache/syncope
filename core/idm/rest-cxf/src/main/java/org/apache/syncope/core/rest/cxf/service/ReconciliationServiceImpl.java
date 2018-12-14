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

import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationServiceImpl extends AbstractServiceImpl implements ReconciliationService {

    @Autowired
    private ReconciliationLogic logic;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Override
    public ReconStatus status(final AnyTypeKind anyTypeKind, final String anyKey, final String resourceKey) {
        return logic.status(
                anyTypeKind,
                getActualKey(anyUtilsFactory.getInstance(anyTypeKind).dao(), anyKey),
                resourceKey);
    }

    @Override
    public void push(
            final AnyTypeKind anyTypeKind,
            final String anyKey,
            final String resourceKey,
            final PushTaskTO pushTask) {

        logic.push(
                anyTypeKind,
                getActualKey(anyUtilsFactory.getInstance(anyTypeKind).dao(), anyKey),
                resourceKey,
                pushTask);
    }

    @Override
    public void pull(
            final AnyTypeKind anyTypeKind,
            final String anyKey,
            final String resourceKey,
            final PullTaskTO pullTask) {

        logic.pull(
                anyTypeKind,
                getActualKey(anyUtilsFactory.getInstance(anyTypeKind).dao(), anyKey),
                resourceKey,
                pullTask);
    }
}
