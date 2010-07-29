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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class RoleTOs extends AbstractBaseBean implements Iterable<RoleTO> {

    private List<RoleTO> roles;

    public RoleTOs() {
        roles = new ArrayList<RoleTO>();
    }

    public List<RoleTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleTO> roles) {
        this.roles = roles;
    }

    @Override
    public Iterator<RoleTO> iterator() {
        return roles.iterator();
    }
}
