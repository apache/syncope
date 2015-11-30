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
package org.apache.syncope.common.rest.api.service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;

/**
 * REST operations for anyObjects.
 */
@Path("anyObjects")
public interface AnyObjectService extends AnyService<AnyObjectTO, AnyObjectPatch> {

    /**
     * Returns a paged list of existing any objects matching the given query, for the given type.
     *
     * @param type any type
     * @param listQuery query conditions
     * @return paged list of existing any objects matching the given query, for the given type
     */
    @GET
    @Produces({ JAXRSService.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<AnyObjectTO> list(@NotNull @MatrixParam("type") String type, @BeanParam AnyListQuery listQuery);

}
