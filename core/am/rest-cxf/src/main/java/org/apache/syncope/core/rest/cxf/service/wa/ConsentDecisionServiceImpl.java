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
package org.apache.syncope.core.rest.cxf.service.wa;

import java.util.List;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.common.rest.api.service.wa.ConsentDecisionService;
import org.apache.syncope.core.logic.wa.ConsentDecisionLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class ConsentDecisionServiceImpl extends AbstractService implements ConsentDecisionService {

    protected final ConsentDecisionLogic logic;

    public ConsentDecisionServiceImpl(final ConsentDecisionLogic logic) {
        this.logic = logic;
    }

    @Override
    public void delete(final String owner, final long id) {
        logic.delete(owner, id);
    }

    @Override
    public void delete(final String owner) {
        logic.delete(owner);
    }

    @Override
    public void deleteAll() {
        logic.deleteAll();
    }

    @Override
    public void store(final String owner, final WAConsentDecision consentDecision) {
        logic.store(owner, consentDecision);
    }

    @Override
    public WAConsentDecision read(final String owner, final String service) {
        return logic.read(owner, service);
    }

    private PagedResult<WAConsentDecision> build(final List<WAConsentDecision> read) {
        PagedResult<WAConsentDecision> result = new PagedResult<>();
        result.setPage(1);
        result.setSize(read.size());
        result.setTotalCount(read.size());
        result.getResult().addAll(read);
        return result;
    }

    @Override
    public PagedResult<WAConsentDecision> read(final String owner) {
        return build(logic.read(owner));
    }

    @Override
    public PagedResult<WAConsentDecision> list() {
        return build(logic.list());
    }
}
