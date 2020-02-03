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

import java.io.InputStream;
import java.util.List;
import javax.validation.ValidationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.spring.security.AuthContextUtils;
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

    @Override
    public Response push(final AnyQuery anyQuery, final CSVPushSpec spec) {
        String realm = StringUtils.prependIfMissing(anyQuery.getRealm(), SyncopeConstants.ROOT_REALM);

        SearchCond searchCond = StringUtils.isBlank(anyQuery.getFiql())
                ? null
                : getSearchCond(anyQuery.getFiql(), realm);

        StreamingOutput sout = (os) -> logic.push(
                searchCond,
                anyQuery.getPage(),
                anyQuery.getSize(),
                getOrderByClauses(anyQuery.getOrderBy()),
                realm,
                spec,
                os);

        return Response.ok(sout).
                type(RESTHeaders.TEXT_CSV).
                header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + AuthContextUtils.getDomain() + ".csv").
                build();
    }

    @Override
    public List<ProvisioningReport> pull(final CSVPullSpec spec, final InputStream csv) {
        return logic.pull(spec, csv);
    }
}
