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
package org.apache.syncope.common.lib.command;

import jakarta.ws.rs.PathParam;
import org.apache.syncope.common.lib.BaseBean;

public class CommandTO implements BaseBean {

    private static final long serialVersionUID = 7711356516501958110L;

    public static class Builder {

        protected CommandTO instance;

        public Builder(final String key) {
            getInstance().setKey(key);
        }

        protected CommandTO newInstance() {
            return new CommandTO();
        }

        protected final CommandTO getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        public Builder args(final CommandArgs args) {
            getInstance().setArgs(args);
            return this;
        }

        public CommandTO build() {
            return getInstance();
        }
    }

    private String key;

    private CommandArgs args;

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public CommandArgs getArgs() {
        return args;
    }

    public void setArgs(final CommandArgs args) {
        this.args = args;
    }
}
