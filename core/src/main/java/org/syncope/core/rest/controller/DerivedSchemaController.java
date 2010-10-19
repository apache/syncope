/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.rest.data.DerivedSchemaDataBinder;

@Controller
@RequestMapping("/derivedSchema")
public class DerivedSchemaController extends AbstractController {

    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;
    @Autowired
    private DerivedSchemaDataBinder derivedSchemaDataBinder;

    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public DerivedSchemaTO create(final HttpServletResponse response,
            @RequestBody final DerivedSchemaTO derivedSchemaTO,
            @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException {

        AbstractDerivedSchema derivedSchema =
                derivedSchemaDataBinder.create(
                derivedSchemaTO,
                getAttributableUtil(kind).newDerivedSchema(),
                getAttributableUtil(kind).getSchemaClass());

        derivedSchema = derivedSchemaDAO.save(derivedSchema);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(HttpServletResponse response,
            @PathVariable("kind") final String kind,
            @PathVariable("schema") final String derivedSchemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).getDerivedSchemaClass();
        AbstractDerivedSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaName, reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaName + "'");

            throw new NotFoundException(derivedSchemaName);
        } else {
            derivedSchemaDAO.delete(derivedSchemaName, reference);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<DerivedSchemaTO> list(@PathVariable("kind") final String kind) {
        Class reference = getAttributableUtil(kind).getDerivedSchemaClass();
        List<AbstractDerivedSchema> derivedAttributeSchemas =
                derivedSchemaDAO.findAll(reference);

        List<DerivedSchemaTO> derivedSchemaTOs =
                new ArrayList<DerivedSchemaTO>(derivedAttributeSchemas.size());
        for (AbstractDerivedSchema derivedSchema : derivedAttributeSchemas) {

            derivedSchemaTOs.add(derivedSchemaDataBinder.getDerivedSchemaTO(
                    derivedSchema));
        }

        return derivedSchemaTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/read/{derivedSchema}")
    public DerivedSchemaTO read(@PathVariable("kind") final String kind,
            @PathVariable("derivedSchema") final String derivedSchemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).getDerivedSchemaClass();
        AbstractDerivedSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaName, reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaName + "'");
            throw new NotFoundException(derivedSchemaName);
        }

        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public DerivedSchemaTO update(
            @RequestBody final DerivedSchemaTO derivedSchemaTO,
            @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        Class reference = getAttributableUtil(kind).getDerivedSchemaClass();
        AbstractDerivedSchema derivedSchema =
                derivedSchemaDAO.find(derivedSchemaTO.getName(), reference);
        if (derivedSchema == null) {
            LOG.error("Could not find derived schema '"
                    + derivedSchemaTO.getName() + "'");
            throw new NotFoundException(derivedSchemaTO.getName());
        }

        derivedSchema = derivedSchemaDataBinder.update(derivedSchemaTO,
                derivedSchema, getAttributableUtil(kind).getSchemaClass());

        derivedSchema = derivedSchemaDAO.save(derivedSchema);
        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }
}
