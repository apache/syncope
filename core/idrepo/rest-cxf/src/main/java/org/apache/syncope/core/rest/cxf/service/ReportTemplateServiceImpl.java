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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.core.logic.ReportTemplateLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportTemplateServiceImpl extends AbstractServiceImpl implements ReportTemplateService {

    @Autowired
    private ReportTemplateLogic logic;

    @Override
    public Response create(final ReportTemplateTO reportTemplateTO) {
        ReportTemplateTO created = logic.create(reportTemplateTO.getKey());
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public ReportTemplateTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public List<ReportTemplateTO> list() {
        return logic.list();
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public Response getFormat(final String key, final ReportTemplateFormat format) {
        String template = logic.getFormat(key, format);
        StreamingOutput sout = (os) -> os.write(template.getBytes());

        return Response.ok(sout).
                type(MediaType.APPLICATION_XML).
                build();
    }

    @Override
    public void setFormat(final String key, final ReportTemplateFormat format, final InputStream templateIn) {
        try {
            logic.setFormat(key, format, IOUtils.toString(templateIn, StandardCharsets.UTF_8.name()));
        } catch (final IOException e) {
            LOG.error("While setting format {} for report template {}", format, key, e);
            throw new InternalServerErrorException("Could not read entity", e);
        }
    }

    @Override
    public void removeFormat(final String key, final ReportTemplateFormat format) {
        logic.setFormat(key, format, null);
    }
}
