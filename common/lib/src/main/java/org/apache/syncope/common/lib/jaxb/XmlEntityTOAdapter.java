/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.lib.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;

public class XmlEntityTOAdapter<E extends EntityTO> extends XmlAdapter<EntityTOType, E> {

    @Override
    @SuppressWarnings("unchecked")
    public E unmarshal(final EntityTOType v) throws Exception {
        E result = null;

        switch (v.getType()) {
            case USER:
            case GROUP:
            case ANY_OBJECT:
            case REALM:
                result = (E) v.getValue();
                break;

            default:
        }

        return result;
    }

    @Override
    public EntityTOType marshal(final E v) throws Exception {
        EntityTOType result = new EntityTOType();
        if (v instanceof UserTO) {
            result.setType(EntityTOType.Type.USER);
        } else if (v instanceof GroupTO) {
            result.setType(EntityTOType.Type.GROUP);
        } else if (v instanceof AnyObjectTO) {
            result.setType(EntityTOType.Type.ANY_OBJECT);
        } else if (v instanceof RealmTO) {
            result.setType(EntityTOType.Type.REALM);
        }
        result.setValue(v);

        return result;
    }

}
