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
package org.apache.syncope.common.lib.patch;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.to.MembershipTO;

@XmlRootElement(name = "membershipPatch")
@XmlType
public class MembershipPatch extends AbstractPatch {

    private static final long serialVersionUID = -6783121761221554433L;

    public static class Builder extends AbstractPatch.Builder<MembershipPatch, Builder> {

        @Override
        protected MembershipPatch newInstance() {
            return new MembershipPatch();
        }

        public Builder membershipTO(final MembershipTO membershipTO) {
            getInstance().setMembershipTO(membershipTO);
            return this;
        }
    }

    private MembershipTO membershipTO;

    public MembershipTO getMembershipTO() {
        return membershipTO;
    }

    public void setMembershipTO(final MembershipTO membershipTO) {
        this.membershipTO = membershipTO;
    }

}
