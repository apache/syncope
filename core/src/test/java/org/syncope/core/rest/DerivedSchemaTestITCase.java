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
package org.syncope.core.rest;

import org.syncope.client.to.DerivedSchemaTOs;
import org.syncope.client.to.DerivedSchemaTO;
import org.junit.Test;
import static org.junit.Assert.*;

public class DerivedSchemaTestITCase extends AbstractTest {

    @Test
    public void derivedList() {
        DerivedSchemaTOs derivedSchemas =
                restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/list.json", DerivedSchemaTOs.class);
        assertFalse(derivedSchemas.getDerivedSchemas().isEmpty());
    }

    @Test
    public void derivedRead() {
        DerivedSchemaTO derivedSchemaTO = restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/read/cn.json", DerivedSchemaTO.class);
        assertNotNull(derivedSchemaTO);
    }
}
