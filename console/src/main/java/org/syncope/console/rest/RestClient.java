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
import java.util.Set;
import org.springframework.web.client.RestTemplate;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;

/**
 * Client for calling rest services.
 */
public class RestClient {

    protected RestTemplate restTemplate;
    protected static final String BASE_URL = "http://192.168.0.137:8080/syncope/";

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<SchemaTO> getAttributesList() {
        List<SchemaTO> attributeSchemas =
                restTemplate.getForObject(BASE_URL + "schema/attribute/role/list.json", List.class);

        return attributeSchemas;
    }

    public List<DerivedSchemaTO> getDerivedAttributesList() {
        List<DerivedSchemaTO> derivedAttributeSchemas =
                restTemplate.getForObject(BASE_URL + "schema/derivedAttribute/role/list.json", List.class);

        return derivedAttributeSchemas;
    }

    public List<SchemaTO> getUserAttributesList() {
        List<SchemaTO> attributeSchemas =
                restTemplate.getForObject(BASE_URL + "schema/attribute/user/list.json", List.class);

        return attributeSchemas;
    }

    public List<DerivedSchemaTO> getUserDerivedAttributesList() {
        List<DerivedSchemaTO> derivedAttributeSchemas =
                restTemplate.getForObject(BASE_URL + "schema/derivedAttribute/user/list.json", List.class);

        return derivedAttributeSchemas;
    }

    /*-- USERS --*/

    public Set<UserTO> getUserList() {
        Set<UserTO> listUsers =
                restTemplate.getForObject(BASE_URL
                + "user/list.json", Set.class);
        return listUsers;
    }

    public UserTO getUser() {
        UserTO userTO =
                restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                UserTO.class, "11");
        return userTO;
    }

}
