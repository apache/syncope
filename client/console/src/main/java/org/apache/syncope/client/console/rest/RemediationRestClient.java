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

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.rest.api.service.RemediationService;

public class RemediationRestClient extends BaseRestClient {

    private static final long serialVersionUID = -7033745375669316378L;

    public List<RemediationTO> getRemediations() {
        return getService(RemediationService.class).list();
    }

    public RemediationTO getRemediation(final String key) {
        return getService(RemediationService.class).read(key);
    }

    public <T extends AnyTO> ProvisioningResult<T> remedy(final String key, final T anyTO) {
        Response response = getService(RemediationService.class).remedy(key, anyTO);
        return response.readEntity(new GenericType<ProvisioningResult<T>>() {
        });
    }

    public <T extends AnyTO> ProvisioningResult<T> remedy(final String key, final AnyPatch anyPatch) {
        Response response = getService(RemediationService.class).remedy(key, anyPatch);
        return response.readEntity(new GenericType<ProvisioningResult<T>>() {
        });
    }

    public ProvisioningResult<? extends AnyTO> remedy(final String key, final String anyKey) {
        Response response = getService(RemediationService.class).remedy(key, anyKey);
        return response.readEntity(new GenericType<ProvisioningResult<? extends AnyTO>>() {
        });
    }

    public void delete(final String remediation) {
        getService(RemediationService.class).delete(remediation);
    }
}
