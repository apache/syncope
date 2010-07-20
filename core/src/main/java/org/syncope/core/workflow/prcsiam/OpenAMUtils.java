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

import java.util.Set;
import org.syncope.core.persistence.beans.role.SyncopeRole;

/**
 * TODO: remove ASAP!
 */
public class OpenAMUtils {

    public static String getRealmFromRoles(Set<SyncopeRole> roles) {
        String realm = "/";

        if (roles.size() > 0) {
            SyncopeRole role = roles.iterator().next();

            while (role.getParent() != null
                    && !role.getParent().getName().equals("/")) {

                role = role.getParent();
            }
            if (role.getParent() != null) {
                realm = role.getName();
            }
        }

        return realm;
    }
}
