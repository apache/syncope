/*
 *  Copyright 2010 luis.
 * 
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
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;

/**
 * Console client for invoking rest schema services.
 */
public class SchemaRestClient
{
    RestClient restClient;

    public List<SchemaTO> getAttributesList() {
        List<SchemaTO> attributeSchemas =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "schema/role/list.json", List.class);

        return attributeSchemas;
    }

    public List<DerivedSchemaTO> getDerivedAttributesList() {
        List<DerivedSchemaTO> derivedAttributeSchemas =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "derivedSchema/role/list.json", List.class);

        return derivedAttributeSchemas;
    }

    public List<SchemaTO> getUserAttributesList() {
        List<SchemaTO> attributeSchemas =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "schema/user/list.json", List.class);

        return attributeSchemas;
    }

    public List<DerivedSchemaTO> getUserDerivedAttributesList() {
        List<DerivedSchemaTO> derivedAttributeSchemas =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "derivedSchema/user/list.json", List.class);

        return derivedAttributeSchemas;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
    
}
