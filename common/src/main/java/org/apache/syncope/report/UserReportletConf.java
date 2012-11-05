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
package org.apache.syncope.report;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.annotation.FormAttributeField;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.types.IntMappingType;

public class UserReportletConf extends AbstractReportletConf {

    public enum Feature {

        id,
        username,
        workflowId,
        status,
        creationDate,
        lastLoginDate,
        changePwdDate,
        passwordHistorySize,
        failedLoginCount,
        memberships,
        resources

    }

    private static final long serialVersionUID = 6602717600064602764L;

    private NodeCond matchingCond;

    @FormAttributeField(schema = IntMappingType.UserSchema)
    private List<String> attrs;

    @FormAttributeField(schema = IntMappingType.UserDerivedSchema)
    private List<String> derAttrs;

    @FormAttributeField(schema = IntMappingType.UserVirtualSchema)
    private List<String> virAttrs;

    private List<Feature> features;

    public UserReportletConf() {
        super();
    }

    public UserReportletConf(final String name) {
        super(name);

        attrs = new ArrayList<String>();
        derAttrs = new ArrayList<String>();
        virAttrs = new ArrayList<String>();
        features = new ArrayList<Feature>();
    }

    public List<String> getAttrs() {
        return attrs;
    }

    public void setAttrs(List<String> attrs) {
        this.attrs = attrs;
    }

    public List<String> getDerAttrs() {
        return derAttrs;
    }

    public void setDerAttrs(List<String> derAttrs) {
        this.derAttrs = derAttrs;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    public NodeCond getMatchingCond() {
        return matchingCond;
    }

    public void setMatchingCond(NodeCond matchingCond) {
        this.matchingCond = matchingCond;
    }

    public List<String> getVirAttrs() {
        return virAttrs;
    }

    public void setVirAttrs(List<String> virAttrs) {
        this.virAttrs = virAttrs;
    }
}
