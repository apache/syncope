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
package org.syncope.client.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;

public class JSONTest {

    @Test
    public void testSearchCondition() throws IOException {
        AttributeCond usernameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond1.setSchema("username");
        usernameLeafCond1.setExpression("%o%");

        AttributeCond usernameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond2.setSchema("username");
        usernameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond1),
                NodeCond.getLeafCond(usernameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, searchCondition);

        NodeCond actual = mapper.readValue(writer.toString(), NodeCond.class);
        assertEquals(searchCondition, actual);
    }
}
