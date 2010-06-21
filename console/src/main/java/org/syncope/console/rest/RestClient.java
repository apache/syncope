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

package org.syncope.console.rest;

import java.util.List;
import org.springframework.web.client.RestTemplate;
import org.syncope.client.to.AttributeSchemaTO;
import org.syncope.client.to.DerivedAttributeSchemaTO;

/**
 * Client for calling rest services.
 */
public class RestClient {

    private RestTemplate restTemplate;
    private static final String BASE_URL = "http://192.168.0.137:8080/syncope/";

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AttributeSchemaTO> getAttributeList() {
        List<AttributeSchemaTO> attributeSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/attribute/list.json", List.class);

        return attributeSchemas;
    }

    public List<DerivedAttributeSchemaTO> derivedAttributeList() {
        List<DerivedAttributeSchemaTO> derivedAttributeSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/derivedAttribute/list.json", List.class);

        return derivedAttributeSchemas;
    }

}
