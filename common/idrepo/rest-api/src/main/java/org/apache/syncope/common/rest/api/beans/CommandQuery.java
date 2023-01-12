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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.QueryParam;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class CommandQuery extends AbstractQuery {

    private static final long serialVersionUID = -8792519310029596796L;

    public static class Builder extends AbstractQuery.Builder<CommandQuery, Builder> {

        @Override
        protected CommandQuery newInstance() {
            return new CommandQuery();
        }

        public Builder keyword(final String keyword) {
            getInstance().setKeyword(keyword);
            return this;
        }
    }

    private String keyword;

    @Parameter(name = JAXRSService.PARAM_KEYWORD, description = "keyword to match", schema =
            @Schema(implementation = String.class))
    public String getKeyword() {
        return keyword;
    }

    @QueryParam(JAXRSService.PARAM_KEYWORD)
    public void setKeyword(final String keyword) {
        this.keyword = keyword;
    }
}
