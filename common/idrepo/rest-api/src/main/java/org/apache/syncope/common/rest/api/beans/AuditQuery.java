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
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class AuditQuery extends AbstractTimeframeQuery {

    private static final long serialVersionUID = -2863334226169614417L;

    public static class Builder extends AbstractTimeframeQuery.Builder<AuditQuery, Builder> {

        @Override
        protected AuditQuery newInstance() {
            return new AuditQuery();
        }

        public Builder entityKey(final String entityKey) {
            getInstance().setEntityKey(entityKey);
            return this;
        }

        public Builder type(final OpEvent.CategoryType type) {
            getInstance().setType(type);
            return this;
        }

        public Builder category(final String category) {
            getInstance().setCategory(category);
            return this;
        }

        public Builder subcategory(final String subcategory) {
            getInstance().setSubcategory(subcategory);
            return this;
        }

        public Builder op(final String op) {
            getInstance().setOp(op);
            return this;
        }

        public Builder outcome(final OpEvent.Outcome outcome) {
            getInstance().setOutcome(outcome);
            return this;
        }
    }

    private String entityKey;

    private OpEvent.CategoryType type;

    private String category;

    private String subcategory;

    private String op;

    private OpEvent.Outcome outcome;

    @Parameter(name = JAXRSService.PARAM_ENTITY_KEY, description = "audit entity key to match", schema =
            @Schema(implementation = String.class, example = "50592942-73ec-44c4-a377-e859524245e4"))
    public String getEntityKey() {
        return entityKey;
    }

    @QueryParam(JAXRSService.PARAM_ENTITY_KEY)
    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Parameter(name = "type", description = "audit type to match", schema =
            @Schema(implementation = OpEvent.CategoryType.class))
    public OpEvent.CategoryType getType() {
        return type;
    }

    @QueryParam("type")
    public void setType(final OpEvent.CategoryType type) {
        this.type = type;
    }

    @Parameter(name = "category", description = "audit category to match", schema =
            @Schema(implementation = String.class))
    public String getCategory() {
        return category;
    }

    @QueryParam("category")
    public void setCategory(final String category) {
        this.category = category;
    }

    @Parameter(name = "subcategory", description = "audit subcategory to match", schema =
            @Schema(implementation = String.class))
    public String getSubcategory() {
        return subcategory;
    }

    @QueryParam("subcategory")
    public void setSubcategory(final String subcategory) {
        this.subcategory = subcategory;
    }

    @Parameter(name = "outcome", description = "audit event outcome to match", schema =
            @Schema(implementation = OpEvent.Outcome.class))
    public OpEvent.Outcome getOutcome() {
        return outcome;
    }

    @QueryParam("outcome")
    public void setOutcome(final OpEvent.Outcome outcome) {
        this.outcome = outcome;
    }

    @Parameter(name = "op", description = "audit event op to match", schema =
            @Schema(implementation = String.class))
    public String getOp() {
        return op;
    }

    @QueryParam("op")
    public void setOp(final String op) {
        this.op = op;
    }
}
