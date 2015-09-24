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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class AnyQuery extends AbstractQuery {

    private static final long serialVersionUID = -371488230250055359L;

    private Boolean details;

    public boolean isDetails() {
        return details == null ? true : details;
    }

    @QueryParam(JAXRSService.PARAM_DETAILS)
    @DefaultValue("true")
    public void setDetails(final boolean details) {
        this.details = details;
    }
}
