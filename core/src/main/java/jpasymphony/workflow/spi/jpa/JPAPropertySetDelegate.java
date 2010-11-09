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
package jpasymphony.workflow.spi.jpa;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.opensymphony.workflow.util.PropertySetDelegate;
import java.util.HashMap;
import java.util.Map;
import jpasymphony.dao.JPAPropertySetItemDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class JPAPropertySetDelegate implements PropertySetDelegate {

    final public static String DAO = "propertySetItemDAO";

    final public static String ENTRY_ID = "entryId";

    @Autowired
    private JPAPropertySetItemDAO propertySetItemDAO;

    @Override
    public PropertySet getPropertySet(long entryId) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(ENTRY_ID, Long.valueOf(entryId));
        args.put(DAO, propertySetItemDAO);

        return PropertySetManager.getInstance("jpa", args);
    }
}
