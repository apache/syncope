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
package org.apache.syncope.common.lib.policy;

import java.io.Serializable;

public class DefaultTicketExpirationPolicyConf implements TicketExpirationPolicyConf {

    private static final long serialVersionUID = -6177632746685440169L;

    public static class TGTConf implements Serializable {

        private static final long serialVersionUID = 4020295776618112940L;

        /**
         * TTL of this ticket, in seconds.
         */
        private long maxTimeToLiveInSeconds;

        public long getMaxTimeToLiveInSeconds() {
            return maxTimeToLiveInSeconds;
        }

        public void setMaxTimeToLiveInSeconds(final long maxTimeToLiveInSeconds) {
            this.maxTimeToLiveInSeconds = maxTimeToLiveInSeconds;
        }
    }

    public static class STConf extends TGTConf {

        private static final long serialVersionUID = -9141008704559934825L;

        /**
         * Number of times this ticket can be used.
         */
        private long numberOfUses;

        public long getNumberOfUses() {
            return numberOfUses;
        }

        public void setNumberOfUses(final long numberOfUses) {
            this.numberOfUses = numberOfUses;
        }
    }

    private TGTConf tgtConf;

    private STConf stConf;

    private TGTConf proxyTgtConf;

    private STConf proxyStConf;

    public TGTConf getTgtConf() {
        return tgtConf;
    }

    public void setTgtConf(final TGTConf tgtConf) {
        this.tgtConf = tgtConf;
    }

    public STConf getStConf() {
        return stConf;
    }

    public void setStConf(final STConf stConf) {
        this.stConf = stConf;
    }

    public TGTConf getProxyTgtConf() {
        return proxyTgtConf;
    }

    public void setProxyTgtConf(final TGTConf proxyTgtConf) {
        this.proxyTgtConf = proxyTgtConf;
    }

    public STConf getProxyStConf() {
        return proxyStConf;
    }

    public void setProxyStConf(final STConf proxySTConf) {
        this.proxyStConf = proxySTConf;
    }
}
