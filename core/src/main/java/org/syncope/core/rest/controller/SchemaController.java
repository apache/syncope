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
import java.util.Collections;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.syncope.client.to.AttributeSchemaTO;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;

@Controller
@RequestMapping("/schema")
public class SchemaController {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaController.class);

    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public Set<AttributeSchemaTO> list(HttpServletRequest request) throws IOException {

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        AttributeSchemaDAO attributeSchemaDAO =
                (AttributeSchemaDAO) webApplicationContext.getBean(
                "attributeSchemaDAOImpl");

        return Collections.singleton(new AttributeSchemaTO());
    }
}
