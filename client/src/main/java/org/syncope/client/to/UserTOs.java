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

public class UserTOs extends AbstractBaseBean implements Iterable<UserTO> {

    private List<UserTO> users;

    public UserTOs() {
        users = new ArrayList<UserTO>();
    }

    public boolean addUser(UserTO userTO) {
        return users.add(userTO);
    }

    public boolean removeUser(UserTO userTO) {
        return users.remove(userTO);
    }

    public List<UserTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserTO> users) {
        this.users = users;
    }

    @Override
    public Iterator<UserTO> iterator() {
        return users.iterator();
    }
}
