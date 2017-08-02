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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.rest.api.service.ConfigurationService;
import org.apache.syncope.core.logic.ConfigurationLogic;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationServiceImpl extends AbstractServiceImpl implements ConfigurationService {

    private static final String CONTENT_XML = "Content.xml";

    @Autowired
    private ConfigurationLogic logic;

    @Override
    public Response export() {
        StreamingOutput sout = (os) -> logic.export(os);

        return Response.ok(sout).
                type(MediaType.TEXT_XML).
                header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + AuthContextUtils.getDomain() + CONTENT_XML).
                build();
    }

    @Override
    public void delete(final String schema) {
        logic.delete(schema);
    }

    @Override
    public List<AttrTO> list() {
        return logic.list();
    }

    @Override
    public AttrTO get(final String schema) {
        return logic.get(schema);
    }

    @Override
    public void set(final AttrTO value) {
        logic.set(value);
    }
}
