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
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.persistence.AbstractTest;

@Transactional
public class AttrTest extends AbstractTest {

    @Autowired
    private AttrDAO attributeDAO;
    @Autowired
    private AttrValueDAO attributeValueDAO;

    @Test
    public final void deleteAttribute() {
        attributeDAO.delete(550L, UAttr.class);

        attributeDAO.flush();

        assertNull(attributeDAO.find(550L, UAttr.class));
        assertNull(attributeValueDAO.find(22L, UAttrValue.class));
    }

    @Test
    public final void deleteAttributeValue() {
        UAttrValue value =
                attributeValueDAO.find(20L, UAttrValue.class);
        int attributeValueNumber =
                value.getAttribute().getValues().size();

        attributeValueDAO.delete(20L, UAttrValue.class);

        attributeValueDAO.flush();

        assertNull(attributeValueDAO.find(20L, UAttrValue.class));

        UAttr attribute = attributeDAO.find(200L, UAttr.class);
        assertEquals(attribute.getValues().size(),
                attributeValueNumber - 1);
    }
}
