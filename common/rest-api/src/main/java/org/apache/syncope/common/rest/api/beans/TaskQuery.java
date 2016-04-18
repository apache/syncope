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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.QueryParam;
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
            switch (getInstance().getType()) {
                case PROPAGATION:
                case PULL:
                case PUSH:
                    getInstance().setResource(resource);
                    return this;

                default:
                    throw new IllegalArgumentException("resource not allowed for " + getInstance().getType());
            }
        }

        public Builder notification(final String notification) {
            switch (getInstance().getType()) {
                case NOTIFICATION:
                    getInstance().setNotification(notification);
                    return this;

                default:
                    throw new IllegalArgumentException("notification not allowed for " + getInstance().getType());
            }
        }

        public Builder anyTypeKind(final AnyTypeKind anyTypeKind) {
            switch (getInstance().getType()) {
                case PROPAGATION:
                case NOTIFICATION:
                    getInstance().setAnyTypeKind(anyTypeKind);
                    return this;

                default:
                    throw new IllegalArgumentException("anyTypeKind not allowed for " + getInstance().getType());
            }
        }

        public Builder anyTypeKey(final String anyTypeKey) {
            switch (getInstance().getType()) {
                case PROPAGATION:
                case NOTIFICATION:
                    getInstance().setAnyTypeKey(anyTypeKey);
                    return this;

                default:
                    throw new IllegalArgumentException("anyTypeKey not allowed for " + getInstance().getType());
            }
        }

        public Builder details(final boolean details) {
            getInstance().setDetails(details);
            return this;
        }

        @Override
        public TaskQuery build() {
            if (getInstance().type == null) {
                throw new IllegalArgumentException("type is required");
            }
            return super.build();
        }

    }

    private TaskType type;

    private String resource;

    private String notification;

    private AnyTypeKind anyTypeKind;

    private String anyTypeKey;

    private Boolean details;

    public TaskType getType() {
        return type;
    }

    @NotNull
    @MatrixParam("type")
    public void setType(final TaskType type) {
        this.type = type;
    }

    public String getResource() {
        return resource;
    }

    @QueryParam(JAXRSService.PARAM_RESOURCE)
    public void setResource(final String resource) {
        this.resource = resource;
    }

    public String getNotification() {
        return notification;
    }

    @QueryParam(JAXRSService.PARAM_NOTIFICATION)
    public void setNotification(final String notification) {
        this.notification = notification;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @QueryParam(JAXRSService.PARAM_ANYTYPE_KIND)
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    @Min(1)
    @QueryParam(JAXRSService.PARAM_ANYTYPE_KEY)
    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    public Boolean getDetails() {
        return details == null ? true : details;
    }

    @QueryParam(JAXRSService.PARAM_DETAILS)
    @DefaultValue("true")
    public void setDetails(final Boolean details) {
        this.details = details;
    }

}
