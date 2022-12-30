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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;

@Provider
public class AddETagFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext reqCtx, final ContainerResponseContext resCtx) throws IOException {
        if (resCtx.getEntityTag() == null) {
            OffsetDateTime lastModified;
            if (resCtx.getEntity() instanceof SCIMUser scimUser) {
                lastModified = scimUser.getMeta().getLastModified();
                if (resCtx.getEntity() instanceof SCIMGroup scimGroup) {
                    lastModified = scimGroup.getMeta().getLastModified();
                }

                if (lastModified != null) {
                    String etagValue = String.valueOf(lastModified.toInstant().toEpochMilli());
                    if (StringUtils.isNotBlank(etagValue)) {
                    resCtx.getHeaders().add(
                            HttpHeaders.ETAG,
                            RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class).
                                    toString(new EntityTag(etagValue)));
                    }
                }
            }
        }
    }
}
