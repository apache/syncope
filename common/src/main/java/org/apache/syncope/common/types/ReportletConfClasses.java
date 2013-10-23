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
package org.apache.syncope.common.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReportletConfClasses {

    private List<String> confClasses;

    public ReportletConfClasses() {
        this.confClasses = new ArrayList<String>();
    }

    public ReportletConfClasses(final Collection<String> confClasses) {
        this();
        this.confClasses.addAll(confClasses);
    }

    public List<String> getConfClasses() {
        return confClasses;
    }

    public void setConfClasses(final List<String> confClasses) {
        this.confClasses = confClasses;
    }
}
