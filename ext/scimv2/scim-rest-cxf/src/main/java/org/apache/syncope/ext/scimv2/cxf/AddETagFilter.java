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
package org.apache.syncope.ext.scimv2.cxf;

import java.io.IOException;
import java.time.OffsetDateTime;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;

@Provider
public class AddETagFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext reqCtx, final ContainerResponseContext resCtx) throws IOException {
        if (resCtx.getEntityTag() == null) {
            OffsetDateTime lastModified;
            if (resCtx.getEntity() instanceof SCIMUser) {
                lastModified = ((SCIMUser) resCtx.getEntity()).getMeta().getLastModified();
                if (resCtx.getEntity() instanceof SCIMGroup) {
                    lastModified = ((SCIMGroup) resCtx.getEntity()).getMeta().getLastModified();
                }

                if (lastModified != null) {
                    String etagValue = String.valueOf(lastModified.toInstant().toEpochMilli());
                    if (StringUtils.isNotBlank(etagValue)) {
                        resCtx.getHeaders().add(HttpHeaders.ETAG, new EntityTag(etagValue, true).toString());
                    }
                }
            }
        }
    }
}
