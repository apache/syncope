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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityExistsException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.rest.data.SchemaDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/schema")
public class SchemaController extends AbstractTransactionalController<SchemaTO> {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SchemaDataBinder binder;

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public SchemaTO create(final HttpServletResponse response, @RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind) {

        if (StringUtils.isBlank(schemaTO.getName())) {
            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.RequiredValuesMissing);
            sce.addElement("Schema name");
            sccee.addException(sce);
            throw sccee;
        }

        AttributableUtil attrUtil = getAttributableUtil(kind);

        if (schemaDAO.find(schemaTO.getName(), attrUtil.schemaClass()) != null) {
            throw new EntityExistsException(attrUtil.schemaClass().getSimpleName()
                    + " '" + schemaTO.getName() + "'");
        }

        AbstractSchema schema = attrUtil.newSchema();
        binder.create(schemaTO, schema);
        schema = schemaDAO.save(schema);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getSchemaTO(schema);
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/delete/{schema}")
    public SchemaTO delete(@PathVariable("kind") final String kind, @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        Class<? extends AbstractSchema> reference = getAttributableUtil(kind).schemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        SchemaTO schemaToDelete = binder.getSchemaTO(schema);
        schemaDAO.delete(schemaName, getAttributableUtil(kind));
        return schemaToDelete;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<SchemaTO> list(@PathVariable("kind") final String kind) {
        AttributableUtil attributableUtil = getAttributableUtil(kind);
        List<AbstractSchema> schemas = schemaDAO.findAll(attributableUtil.schemaClass());

        List<SchemaTO> schemaTOs = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            schemaTOs.add(binder.getSchemaTO(schema));
        }

        return schemaTOs;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/read/{schema}")
    public SchemaTO read(@PathVariable("kind") final String kind, @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaName, attributableUtil.schemaClass());
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        return binder.getSchemaTO(schema);
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public SchemaTO update(@RequestBody final SchemaTO schemaTO, @PathVariable("kind") final String kind)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaTO.getName(), attributableUtil.schemaClass());
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaTO.getName() + "'");
        }

        binder.update(schemaTO, schema, attributableUtil);
        return binder.getSchemaTO(schemaDAO.save(schema));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SchemaTO resolveReference(final Method method, final Object... args) throws
            UnresolvedReferenceException {
        String kind = null;
        String name = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; (name == null || kind == null) && i < args.length; i++) {
                if (args[i] instanceof String) {
                    if (kind == null) {
                        kind = (String) args[i];
                    } else {
                        name = (String) args[i];
                    }
                } else if (args[i] instanceof SchemaTO) {
                    name = ((SchemaTO) args[i]).getName();
                }
            }
        }

        if (name != null) {
            try {
                return binder.getSchemaTO(schemaDAO.find(name, getAttributableUtil(kind).schemaClass()));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
