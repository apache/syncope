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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.Optional;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class TaskQuery extends AbstractQuery {

    private static final long serialVersionUID = -8792519310029596796L;

    public static class Builder extends AbstractQuery.Builder<TaskQuery, Builder> {

        public Builder(final TaskType type) {
            super();
            getInstance().setType(type);
        }

        @Override
        protected TaskQuery newInstance() {
            return new TaskQuery();
        }

        public Builder resource(final String resource) {
            getInstance().setResource(resource);
            return this;
        }

        public Builder notification(final String notification) {
            getInstance().setNotification(notification);
            return this;
        }

        public Builder anyTypeKind(final AnyTypeKind anyTypeKind) {
            getInstance().setAnyTypeKind(anyTypeKind);
            return this;
        }

        public Builder entityKey(final String entityKey) {
            getInstance().setEntityKey(entityKey);
            return this;
        }

        public Builder details(final boolean details) {
            getInstance().setDetails(details);
            return this;
        }
    }

    private TaskType type;

    private String resource;

    private String notification;

    private AnyTypeKind anyTypeKind;

    private String entityKey;

    private Boolean details;

    public TaskType getType() {
        return type;
    }

    @NotNull
    @PathParam("type")
    public void setType(final TaskType type) {
        this.type = type;
    }

    @Parameter(name = JAXRSService.PARAM_RESOURCE, description = "resource key to match", schema =
            @Schema(implementation = String.class, example = "resource-ldap"))
    public String getResource() {
        return resource;
    }

    @QueryParam(JAXRSService.PARAM_RESOURCE)
    public void setResource(final String resource) {
        this.resource = resource;
    }

    @Parameter(name = JAXRSService.PARAM_NOTIFICATION, description = "notification key to match", schema =
            @Schema(implementation = String.class, example = "4bf255f1-85a0-43d6-8988-128dad646f08"))
    public String getNotification() {
        return notification;
    }

    @QueryParam(JAXRSService.PARAM_NOTIFICATION)
    public void setNotification(final String notification) {
        this.notification = notification;
    }

    @Parameter(name = JAXRSService.PARAM_ANYTYPE_KIND, description = "entity type to match", schema =
            @Schema(implementation = AnyTypeKind.class))
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @QueryParam(JAXRSService.PARAM_ANYTYPE_KIND)
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Parameter(name = JAXRSService.PARAM_ENTITY_KEY, description = "entity key to match", schema =
            @Schema(implementation = String.class, example = "50592942-73ec-44c4-a377-e859524245e4"))
    public String getEntityKey() {
        return entityKey;
    }

    @QueryParam(JAXRSService.PARAM_ENTITY_KEY)
    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Parameter(name = JAXRSService.PARAM_DETAILS, description = "whether detailed information about executions is to "
            + "be included", schema =
            @Schema(implementation = Boolean.class))
    public Boolean getDetails() {
        return Optional.ofNullable(details).orElse(Boolean.TRUE);
    }

    @QueryParam(JAXRSService.PARAM_DETAILS)
    @DefaultValue("true")
    public void setDetails(final Boolean details) {
        this.details = details;
    }
}
