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
package org.apache.syncope.core.logic;

import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnyCRUDLogicOp<TO extends AnyTO, C extends AnyCR, U extends AnyUR> {

    TO read(String key);

    Page<TO> search(
            SearchCond searchCond,
            Pageable pageable,
            String realm,
            boolean recursive,
            boolean details);

    ProvisioningResult<TO> create(C createReq, boolean nullPriorityAsync);

    ProvisioningResult<TO> update(U updateReq, boolean nullPriorityAsync);

    ProvisioningResult<TO> delete(String key, boolean nullPriorityAsync);

    TO unlink(String key, Collection<String> resources);

    TO link(String key, Collection<String> resources);

    ProvisioningResult<TO> unassign(
            String key,
            Collection<String> resources,
            boolean nullPriorityAsync);

    ProvisioningResult<TO> assign(
            String key,
            Collection<String> resources,
            boolean changepwd,
            String password,
            boolean nullPriorityAsync);

    ProvisioningResult<TO> deprovision(
            String key,
            List<String> resources,
            boolean nullPriorityAsync);

    ProvisioningResult<TO> provision(
            String key,
            List<String> resources,
            boolean changepwd,
            String password,
            boolean nullPriorityAsync);
}
