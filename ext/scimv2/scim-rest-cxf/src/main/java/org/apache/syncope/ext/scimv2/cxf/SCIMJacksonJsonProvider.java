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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.text.SimpleDateFormat;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;

@Provider
@Consumes(SCIMConstants.APPLICATION_SCIM_JSON)
@Produces(SCIMConstants.APPLICATION_SCIM_JSON)
public class SCIMJacksonJsonProvider extends JacksonJsonProvider {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern(SyncopeConstants.DEFAULT_DATE_PATTERN);
        return sdf;
    });

    public SCIMJacksonJsonProvider() {
        super(new ObjectMapper(), BASIC_ANNOTATIONS);
        _mapperConfig.getConfiguredMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        _mapperConfig.getConfiguredMapper().setDateFormat(DATE_FORMAT.get());
    }
}
