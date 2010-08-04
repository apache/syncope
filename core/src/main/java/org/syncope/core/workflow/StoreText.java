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
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import java.util.Collections;
import java.util.Map;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;

public class StoreText extends AbstractStoreAttributeValue {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        AbstractAttribute attribute = getAttribute(transientVars, args);

        String text = (String) transientVars.get(args.get("schema"));
        if (text == null) {
            throw new WorkflowException("Missing text");
        }

        AbstractAttributeValue textAttributeValue =
                attributableUtil.newAttributeValue();
        textAttributeValue.setStringValue(text);
        textAttributeValue.setAttribute(attribute);
        if (attribute.getSchema().isMultivalue()) {
            attribute.addAttributeValue(textAttributeValue);
        } else {
            attribute.setAttributeValues(
                    Collections.singletonList(textAttributeValue));
        }
    }
}
