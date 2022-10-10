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

public class CommandOutput extends CommandTO {

    private static final long serialVersionUID = 7711356516501958110L;

    public static class Builder extends CommandTO.Builder {

        public Builder(final String key) {
            super(key);
        }

        public Builder(final CommandTO commandTO) {
            super(commandTO.getKey());
            args(commandTO.getArgs());
        }

        @Override
        protected CommandOutput newInstance() {
            return new CommandOutput();
        }

        @Override
        public Builder args(final CommandArgs args) {
            return (Builder) super.args(args);
        }

        public Builder output(final String output) {
            ((CommandOutput) getInstance()).setOutput(output);
            return this;
        }

        @Override
        public CommandOutput build() {
            return (CommandOutput) super.build();
        }
    }

    private String output;

    public String getOutput() {
        return output;
    }

    public void setOutput(final String output) {
        this.output = output;
    }
}
