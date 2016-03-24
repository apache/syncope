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

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;

/**
 * REST operations for groups.
 */
@Path("groups")
public interface GroupService extends AnyService<GroupTO, GroupPatch> {

    /**
     * This method is similar to read() but uses different authentication handling to ensure that a user
     * can read his own groups.
     *
     * @return own groups
     */
    @GET
    @Path("own")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<GroupTO> own();

    /**
     * Returns a paged list of existing groups matching the given query.
     *
     * @param listQuery query conditions
     * @return paged list of existing groups matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedResult<GroupTO> list(@BeanParam AnyListQuery listQuery);
}
