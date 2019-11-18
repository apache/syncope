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
package org.apache.syncope.common.rest.api.beans;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class AnyQuery extends AbstractQuery {

    private static final long serialVersionUID = -6736562952418964707L;

    public static class Builder extends AbstractQuery.Builder<AnyQuery, Builder> {

        @Override
        protected AnyQuery newInstance() {
            return new AnyQuery();
        }

        public Builder details(final boolean details) {
            getInstance().setDetails(details);
            return this;
        }

        public Builder realm(final String realm) {
            getInstance().setRealm(realm);
            return this;
        }

        public Builder fiql(final String fiql) {
            getInstance().setFiql(fiql);

            return this;
        }
    }

    private String realm;

    private Boolean details;

    private String fiql;

    @Parameter(name = JAXRSService.PARAM_REALM, description = "realms define a hierarchical security domain tree, "
            + "primarily meant for containing Users, Groups and Any Objects", schema =
            @Schema(implementation = String.class, defaultValue = SyncopeConstants.ROOT_REALM, externalDocs =
                    @ExternalDocumentation(description = "Apache Syncope Reference Guide",
                            url = "http://syncope.apache.org/docs/2.1/reference-guide.html#realms")))
    public String getRealm() {
        return realm;
    }

    @DefaultValue(SyncopeConstants.ROOT_REALM)
    @QueryParam(JAXRSService.PARAM_REALM)
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @Parameter(name = JAXRSService.PARAM_DETAILS, description = "whether detailed information is to be included, "
            + "if applicable, about virtual attributes, (dynamic) roles, privileges, relationships, "
            + "(dynamic) memberships or linked accounts", schema =
            @Schema(implementation = Boolean.class))
    public Boolean getDetails() {
        return details == null ? Boolean.TRUE : details;
    }

    @QueryParam(JAXRSService.PARAM_DETAILS)
    @DefaultValue("true")
    public void setDetails(final Boolean details) {
        this.details = details;
    }

    public String getFiql() {
        return fiql;
    }

    @Parameter(name = JAXRSService.PARAM_FIQL, description = "Feed Item Query Language (FIQL, pronounced “fickle”) is "
            + "a simple but flexible, URI-friendly syntax for expressing filters across the entries in a syndicated "
            + "feed.", example = "username==rossini", schema =
            @Schema(implementation = String.class, externalDocs =
                    @ExternalDocumentation(description = "Apache Syncope Reference Guide",
                            url = "http://syncope.apache.org/docs/2.1/reference-guide.html#search")))
    @QueryParam(JAXRSService.PARAM_FIQL)
    public void setFiql(final String fiql) {
        this.fiql = fiql;
    }
}
