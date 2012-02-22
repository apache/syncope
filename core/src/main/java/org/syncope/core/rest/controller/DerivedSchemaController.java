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
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.rest.data.DerivedSchemaDataBinder;

@Controller
@RequestMapping("/derivedSchema")
public class DerivedSchemaController extends AbstractController {

    @Autowired
    private DerSchemaDAO derivedSchemaDAO;

    @Autowired
    private DerivedSchemaDataBinder derivedSchemaDataBinder;

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/{kind}/create")
    public DerivedSchemaTO create(final HttpServletResponse response,
            @RequestBody final DerivedSchemaTO derivedSchemaTO,
            @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException {

        AbstractDerSchema derivedSchema =
                derivedSchemaDataBinder.create(derivedSchemaTO,
                getAttributableUtil(kind).newDerivedSchema());

        derivedSchema = derivedSchemaDAO.save(derivedSchema);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(HttpServletResponse response,
            @PathVariable("kind") final String kind,
            @PathVariable("schema") final String derivedSchemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).derivedSchemaClass();
        AbstractDerSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaName, reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaName + "'");

            throw new NotFoundException(derivedSchemaName);
        } else {
            derivedSchemaDAO.delete(
                    derivedSchemaName, getAttributableUtil(kind));
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list")
    public List<DerivedSchemaTO> list(@PathVariable("kind") final String kind) {
        Class reference = getAttributableUtil(kind).derivedSchemaClass();
        List<AbstractDerSchema> derivedAttributeSchemas =
                derivedSchemaDAO.findAll(reference);

        List<DerivedSchemaTO> derivedSchemaTOs =
                new ArrayList<DerivedSchemaTO>(derivedAttributeSchemas.size());
        for (AbstractDerSchema derivedSchema : derivedAttributeSchemas) {

            derivedSchemaTOs.add(derivedSchemaDataBinder.getDerivedSchemaTO(
                    derivedSchema));
        }

        return derivedSchemaTOs;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/read/{derivedSchema}")
    public DerivedSchemaTO read(@PathVariable("kind") final String kind,
            @PathVariable("derivedSchema") final String derivedSchemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).derivedSchemaClass();
        AbstractDerSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaName, reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaName + "'");
            throw new NotFoundException(derivedSchemaName);
        }

        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/{kind}/update")
    public DerivedSchemaTO update(
            @RequestBody final DerivedSchemaTO derivedSchemaTO,
            @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        Class reference = getAttributableUtil(kind).derivedSchemaClass();
        AbstractDerSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaTO.getName(), reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaTO.getName() + "'");
            throw new NotFoundException(derivedSchemaTO.getName());
        }

        derivedSchema = derivedSchemaDataBinder.update(derivedSchemaTO,
                derivedSchema);

        derivedSchema = derivedSchemaDAO.save(derivedSchema);
        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }
}
