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
package org.syncope.core.persistence.beans;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import org.hibernate.annotations.Type;
import org.syncope.core.persistence.validation.entity.PolicyCheck;
import org.syncope.types.AbstractPolicy;
import org.syncope.types.PolicyType;

@Entity
@PolicyCheck
public class Policy extends AbstractBaseBean {

    private static final long serialVersionUID = -5844833125843247458L;

    @Id
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyType type;

    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String specification;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public <T extends AbstractPolicy> T getSpecification() {
        T result = null;

        try {
            ByteArrayInputStream tokenContentIS = new ByteArrayInputStream(
                    URLDecoder.decode(specification, "UTF-8").getBytes());

            XMLDecoder decoder = new XMLDecoder(tokenContentIS);
            Object object = decoder.readObject();
            decoder.close();

            result = (T) object;
        } catch (Throwable t) {
            LOG.error("During connector properties deserialization", t);
        }

        return result;
    }

    public <T extends AbstractPolicy> void setSpecification(final T policy) {
        try {
            ByteArrayOutputStream tokenContentOS = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(tokenContentOS);
            encoder.writeObject(policy);
            encoder.flush();
            encoder.close();

            specification = URLEncoder.encode(
                    tokenContentOS.toString(), "UTF-8");

        } catch (Throwable t) {
            LOG.error("During connector properties serialization", t);
        }
    }
}
