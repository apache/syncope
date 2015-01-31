package org.apache.syncope.common.lib.to;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.SubjectType;

@XmlRootElement(name = "camelRoute")
@XmlType
public class CamelRouteTO extends AbstractBaseBean {

    private String name;

    private SubjectType subjectType;

    private String content;

    public String getKey() {
        return name;
    }

    public void setKey(final String key) {
        this.name = key;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(final SubjectType subjectType) {
        this.subjectType = subjectType;
    }

}
