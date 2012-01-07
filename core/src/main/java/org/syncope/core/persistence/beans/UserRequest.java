/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.persistence.beans;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.util.XMLSerializer;
import org.syncope.types.UserRequestType;

@Entity
public class UserRequest extends AbstractBaseBean {

    private static final long serialVersionUID = 4977358381988835119L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Lob
    //@Type(type = "org.hibernate.type.StringClobType")
    private String payload;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRequestType type;

    public Long getId() {
        return id;
    }

    public UserRequestType getType() {
        return type;
    }

    public UserTO getUserTO() {
        return type != UserRequestType.CREATE
                ? null
                : XMLSerializer.<UserTO>deserialize(payload);
    }

    public void setUserTO(final UserTO userTO) {
        type = UserRequestType.CREATE;
        payload = XMLSerializer.serialize(userTO);
    }

    public UserMod getUserMod() {
        return type != UserRequestType.UPDATE
                ? null
                : XMLSerializer.<UserMod>deserialize(payload);
    }

    public void setUserMod(final UserMod userMod) {
        type = UserRequestType.UPDATE;
        payload = XMLSerializer.serialize(userMod);
    }

    public Long getUserId() {
        return type != UserRequestType.DELETE
                ? null
                : Long.valueOf(payload);
    }

    public void setUserId(final Long userId) {
        type = UserRequestType.DELETE;
        payload = String.valueOf(userId);
    }
}
