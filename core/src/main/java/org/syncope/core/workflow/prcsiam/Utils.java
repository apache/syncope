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
package org.syncope.core.workflow.prcsiam;

import com.opensymphony.workflow.WorkflowException;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;

public class Utils {

    public static String getUserId(UserTO userTO) throws WorkflowException {
        String userId = null;

        for (AttributeTO attributeTO : userTO.getAttributes()) {
            if (attributeTO.getSchema().equals("userId")
                    && attributeTO.getValues() != null
                    && !attributeTO.getValues().isEmpty()) {

                userId = attributeTO.getValues().iterator().next();
            }
        }
        if (userId == null) {
            throw new WorkflowException(
                    "UserTO not provided with mandatory userId: " + userTO);
        }

        return userId;
    }

    public static String getUserId(SyncopeUser syncopeUser)
            throws WorkflowException {

        UserAttribute userId = syncopeUser.getAttribute("userId");
        if (userId == null || userId.getAttributeValues().isEmpty()) {
            throw new WorkflowException(
                    "SyncopeUser not provided with mandatory userId: "
                    + syncopeUser);
        }

        return userId.getAttributeValues().iterator().next().getStringValue();
    }
}
