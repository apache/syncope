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
package org.apache.syncope.client.console.rest;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.rest.api.beans.RemediationQuery;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class RemediationRestClient extends BaseRestClient {

    private static final long serialVersionUID = -7033745375669316378L;

    public long countRemediations() {
        return getService(RemediationService.class).
                list(new RemediationQuery.Builder().page(1).size(0).build()).
                getTotalCount();
    }

    public List<RemediationTO> getRemediations(final int page, final int size, final SortParam<String> sort) {
        return getService(RemediationService.class).
                list(new RemediationQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
    }

    public RemediationTO getRemediation(final String key) {
        return getService(RemediationService.class).read(key);
    }

    public <C extends AnyCR, A extends AnyTO> ProvisioningResult<A> remedy(final String key, final C anyCR) {
        Response response = getService(RemediationService.class).remedy(key, anyCR);
        return response.readEntity(new GenericType<>() {
        });
    }

    public <T extends AnyTO> ProvisioningResult<T> remedy(final String key, final AnyUR anyUR) {
        Response response = getService(RemediationService.class).remedy(key, anyUR);
        return response.readEntity(new GenericType<>() {
        });
    }

    public ProvisioningResult<? extends AnyTO> remedy(final String key, final String anyKey) {
        Response response = getService(RemediationService.class).remedy(key, anyKey);
        return response.readEntity(new GenericType<>() {
        });
    }

    public void delete(final String remediation) {
        getService(RemediationService.class).delete(remediation);
    }
}
