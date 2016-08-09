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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import java.util.ArrayList;
import java.util.List;

public class TagInfo {

    private String tagName;
    private boolean hasBody;
    private boolean emptyTag;
    private String description;
    private List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
    private List<String> children = new ArrayList<String>();
    public static final int NONE  = 0;
    public static final int EVENT = 1;
    public static final int FORM  = 2;

    public TagInfo(final String tagName, final boolean hasBody) {
        this(tagName, hasBody, false);
    }

    public TagInfo(final String tagName, final boolean hasBody, final boolean emptyTag) {
        this.tagName = tagName;
        this.hasBody = hasBody;
        this.emptyTag = emptyTag;
    }

    public String getTagName() {
        return this.tagName;
    }

    public boolean hasBody() {
        return this.hasBody;
    }

    public boolean isEmptyTag() {
        return this.emptyTag;
    }

    public void addAttributeInfo(final AttributeInfo attribute) {
        int i = 0;
        for ( ; i < attributes.size(); i++) {
            AttributeInfo info = attributes.get(i);
            if (info.getAttributeName().compareTo(attribute.getAttributeName()) > 0) {
                break;
            }
        }
        this.attributes.add(i, attribute);
    }

    public AttributeInfo[] getAttributeInfo() {
        return this.attributes.toArray(new AttributeInfo[this.attributes.size()]);
    }

    public AttributeInfo[] getRequiredAttributeInfo() {
        ArrayList<AttributeInfo> list = new ArrayList<AttributeInfo>();
        for (int i = 0; i < attributes.size(); i++) {
            AttributeInfo info = (AttributeInfo) attributes.get(i);
            if (info.isRequired()) {
                list.add(info);
            }
        }
        return list.toArray(new AttributeInfo[list.size()]);
    }

    public AttributeInfo getAttributeInfo(final String name) {
        for (int i = 0 ; i < attributes.size() ; i++) {
            AttributeInfo info = attributes.get(i);
            if (info.getAttributeName().equals(name)) {
                return info;
            }
        }
        return null;
    }

    public void addChildTagName(final String name) {
        children.add(name);
    }

    public String[] getChildTagNames() {
        return children.toArray(new String[children.size()]);
    }

    @Override public boolean equals(final Object obj) {
        if (obj instanceof TagInfo) {
            TagInfo tagInfo = (TagInfo) obj;
            if (tagInfo.getTagName().equals(getTagName())) {
                return true;
            }
        }
        return false;
    }

    @Override public int hashCode() {
        return this.getTagName().hashCode();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDisplayString() {
        return getTagName();
    }

}
