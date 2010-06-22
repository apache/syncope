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
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.core.persistence.Attributable;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;

@Controller
@RequestMapping("/schema")
public class SchemaController {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaController.class);
    private SchemaDAO schemaDAO;
    private DerivedSchemaDAO derivedSchemaDAO;

    @Autowired
    public SchemaController(SchemaDAO schemaDAO,
            DerivedSchemaDAO derivedSchemaDAO) {

        this.schemaDAO = schemaDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
    }

    private Class getSchemaReference(String kind) {
        Class result = null;

        try {
            result = Attributable.valueOf(kind.toUpperCase()).getSchemaClass();
        } catch (Exception e) {
            log.error("Attributable not supported: " + kind);
            throw new TypeMismatchException(kind, Attributable.class, e);
        }

        return result;
    }

    /*@RequestMapping(method = RequestMethod.POST,
    value = "/attribute/")
    public SchemaTO attributeCreate(HttpServletResponse response,
    @RequestBody SchemaTO schemaTO) {
    }*/
    @RequestMapping(method = RequestMethod.GET,
    value = "/attribute/{kind}/list")
    public List<SchemaTO> attributeList(HttpServletRequest request,
            @PathVariable("kind") String kind) {

        Class reference = getSchemaReference(kind);

        List<AbstractSchema> schemas = schemaDAO.findAll(reference);

        List<SchemaTO> result = new ArrayList<SchemaTO>(schemas.size());
        SchemaTO schemaTO = null;
        String[] ignoreProperties = {"derivedSchemas"};
        for (AbstractSchema schema : schemas) {
            schemaTO = new SchemaTO();
            BeanUtils.copyProperties(schema, schemaTO,
                    ignoreProperties);

            for (AbstractDerivedSchema derivedSchema :
                    schema.getDerivedSchemas()) {

                schemaTO.addDerivedSchema(derivedSchema.getName());
            }

            result.add(schemaTO);
        }

        return result;
    }

    private Class getDerivedSchemaReference(String kind) {
        Class result = null;

        try {
            result = Attributable.valueOf(kind.toUpperCase()).getDerivedSchemaClass();
        } catch (Exception e) {
            log.error("Attributable not supported: " + kind);
            throw new TypeMismatchException(kind, Attributable.class, e);
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/derivedAttribute/{kind}/list")
    public List<DerivedSchemaTO> derivedAttributeList(
            HttpServletRequest request, @PathVariable("kind") String kind) {

        Class reference = getDerivedSchemaReference(kind);

        List<AbstractDerivedSchema> derivedAttributeSchemas =
                derivedSchemaDAO.findAll(reference);
        List<DerivedSchemaTO> result =
                new ArrayList<DerivedSchemaTO>(
                derivedAttributeSchemas.size());
        DerivedSchemaTO derivedAttributeSchemaTO = null;
        String[] ignoreProperties = {"schemas"};
        for (AbstractDerivedSchema derivedSchema : derivedAttributeSchemas) {

            derivedAttributeSchemaTO = new DerivedSchemaTO();
            BeanUtils.copyProperties(derivedSchema,
                    derivedAttributeSchemaTO, ignoreProperties);

            for (AbstractSchema schema : derivedSchema.getSchemas()) {
                derivedAttributeSchemaTO.addSchema(schema.getName());
            }

            result.add(derivedAttributeSchemaTO);
        }

        return result;
    }
}
