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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.SAML2BindingType;

@XmlRootElement(name = "saml2idp")
@XmlType
public class SAML2IdPTO extends AbstractBaseBean implements EntityTO {

    private static final long serialVersionUID = 4426527052873779881L;

    private String key;

    private String entityID;

    private String name;

    private String metadata;

    private boolean useDeflateEncoding;

    private SAML2BindingType bindingType;

    private boolean logoutSupported;

    private final List<MappingItemTO> mappingItems = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(final String entityID) {
        this.entityID = entityID;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    public boolean isUseDeflateEncoding() {
        return useDeflateEncoding;
    }

    public void setUseDeflateEncoding(final boolean useDeflateEncoding) {
        this.useDeflateEncoding = useDeflateEncoding;
    }

    public SAML2BindingType getBindingType() {
        return bindingType;
    }

    public void setBindingType(final SAML2BindingType bindingType) {
        this.bindingType = bindingType;
    }

    public boolean isLogoutSupported() {
        return logoutSupported;
    }

    public void setLogoutSupported(final boolean logoutSupported) {
        this.logoutSupported = logoutSupported;
    }

    public MappingItemTO getConnObjectKeyItem() {
        return IterableUtils.find(getMappingItems(), new Predicate<MappingItemTO>() {

            @Override
            public boolean evaluate(final MappingItemTO item) {
                return item.isConnObjectKey();
            }
        });
    }

    protected boolean addConnObjectKeyItem(final MappingItemTO connObjectItem) {
        connObjectItem.setMandatoryCondition("true");
        connObjectItem.setConnObjectKey(true);

        return this.add(connObjectItem);
    }

    public boolean setConnObjectKeyItem(final MappingItemTO connObjectKeyItem) {
        return connObjectKeyItem == null
                ? remove(getConnObjectKeyItem())
                : addConnObjectKeyItem(connObjectKeyItem);
    }

    @XmlElementWrapper(name = "mappingItems")
    @XmlElement(name = "mappingItem")
    @JsonProperty("mappingItems")
    public List<MappingItemTO> getMappingItems() {
        return mappingItems;
    }

    public boolean add(final MappingItemTO item) {
        return item == null ? false : this.mappingItems.contains(item) || this.mappingItems.add(item);
    }

    public boolean remove(final MappingItemTO item) {
        return this.mappingItems.remove(item);
    }

}
