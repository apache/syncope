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
package org.apache.syncope.common.lib.types;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@XmlRootElement(name = "gatewayRoutePredicate")
@XmlType
public class GatewayRoutePredicate implements Serializable {

    private static final long serialVersionUID = -635785645207375128L;

    public static class Builder {

        private final GatewayRoutePredicate instance = new GatewayRoutePredicate();

        public Builder negate() {
            instance.setNegate(true);
            return this;
        }

        public Builder cond(final PredicateCond cond) {
            instance.setCond(cond);
            return this;
        }

        public Builder factory(final PredicateFactory factory) {
            instance.setFactory(factory);
            return this;
        }

        public Builder args(final String args) {
            instance.setArgs(args);
            return this;
        }

        public GatewayRoutePredicate build() {
            return instance;
        }
    }

    private boolean negate;

    private PredicateCond cond;

    private PredicateFactory factory;

    private String args;

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(final boolean negate) {
        this.negate = negate;
    }

    public PredicateCond getCond() {
        return cond;
    }

    public void setCond(final PredicateCond cond) {
        this.cond = cond;
    }

    public PredicateFactory getFactory() {
        return factory;
    }

    public void setFactory(final PredicateFactory factory) {
        this.factory = factory;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(final String args) {
        this.args = args;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(cond).
                append(factory).
                append(args).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GatewayRoutePredicate other = (GatewayRoutePredicate) obj;
        return new EqualsBuilder().
                append(cond, other.cond).
                append(factory, other.factory).
                append(args, other.args).
                build();
    }

    @Override
    public String toString() {
        return "GatewayPredicate{"
                + "negate=" + negate
                + ", cond=" + cond
                + ", factory=" + factory
                + ", args=" + args
                + '}';
    }
}
