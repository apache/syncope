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
package org.apache.syncope.core.rest.cxf;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;

/**
 * Adds the {@code ETag} header to any response containing an instance of {@link AnyTO} as entity.
 * The actual ETag value is computed on the basis of last change date (or creation date if not available).
 */
@Provider
public class AddETagFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext reqCtx, final ContainerResponseContext resCtx) throws IOException {
        if (resCtx.getEntityTag() == null) {
            AnyTO annotated = null;
            if (resCtx.getEntity() instanceof AnyTO) {
                annotated = (AnyTO) resCtx.getEntity();
            } else if (resCtx.getEntity() instanceof ProvisioningResult) {
                EntityTO entity = ((ProvisioningResult<?>) resCtx.getEntity()).getEntity();
                if (entity instanceof AnyTO) {
                    annotated = (AnyTO) entity;
                }
            }
            if (annotated != null) {
                String etagValue = annotated.getETagValue();
                if (StringUtils.isNotBlank(etagValue)) {
                    resCtx.getHeaders().add(HttpHeaders.ETAG, new EntityTag(etagValue).toString());
                }
            }
        }
    }
}
