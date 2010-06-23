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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.DerivedSchemaTO;
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
    public DerivedSchemaTO create(HttpServletResponse response,
            @RequestBody DerivedSchemaTO derivedSchemaTO,
            @PathVariable("kind") String kind)
            throws IOException {

        Class reference = getAttributable(kind).getDerivedSchemaClass();
        AbstractDerivedSchema derivedSchema = null;
        try {
            derivedSchema = derivedSchemaDataBinder.createDerivedSchema(
                    derivedSchemaTO, reference,
                    getAttributable(kind).getSchemaClass());
        } catch (Exception e) {
            log.error("Could not crate for  " + derivedSchemaTO, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return derivedSchemaDataBinder.getDerivedSchemaTO(derivedSchema);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<DerivedSchemaTO> list(HttpServletRequest request,
            @PathVariable("kind") String kind) {

        Class reference = getAttributable(kind).getDerivedSchemaClass();
        List<AbstractDerivedSchema> derivedAttributeSchemas =
                derivedSchemaDAO.findAll(reference);

        List<DerivedSchemaTO> result =
                new ArrayList<DerivedSchemaTO>(derivedAttributeSchemas.size());
        for (AbstractDerivedSchema derivedSchema : derivedAttributeSchemas) {

            result.add(derivedSchemaDataBinder.getDerivedSchemaTO(
                    derivedSchema));
        }

        return result;
    }
}
