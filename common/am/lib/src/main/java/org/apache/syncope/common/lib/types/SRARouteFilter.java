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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class SRARouteFilter implements BaseBean {

    private static final long serialVersionUID = -635785645207375128L;

    public static class Builder {

        private final SRARouteFilter instance = new SRARouteFilter();

        public Builder factory(final SRARouteFilterFactory factory) {
            instance.setFactory(factory);
            return this;
        }

        public Builder args(final String args) {
            instance.setArgs(args);
            return this;
        }

        public SRARouteFilter build() {
            return instance;
        }
    }

    private SRARouteFilterFactory factory;

    private String args;

    public SRARouteFilterFactory getFactory() {
        return factory;
    }

    public void setFactory(final SRARouteFilterFactory factory) {
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
        final SRARouteFilter other = (SRARouteFilter) obj;
        return new EqualsBuilder().
                append(factory, other.factory).
                append(args, other.args).
                build();
    }

    @Override
    public String toString() {
        return "GatewayFilter{"
                + "factory=" + factory
                + ", args=" + args
                + '}';
    }
}
