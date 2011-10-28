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
package org.syncope.client.mod;

import org.syncope.client.AbstractBaseBean;

/**
 * This class is used to specify the willing to modify an external reference id.
 * Use 'null' ReferenceMod to keep the current reference id; use a ReferenceMod
 * with a null id to try to reset the reference id; use a RefernceMod with a
 * not null id to specify a new reference id.
 */
public class ReferenceMod extends AbstractBaseBean {

    private static final long serialVersionUID = -4188817853738067677L;

    private Long id;

    public ReferenceMod() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
