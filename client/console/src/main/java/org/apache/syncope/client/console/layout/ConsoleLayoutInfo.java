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
package org.apache.syncope.client.console.layout;

import java.io.Serializable;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;

public class ConsoleLayoutInfo implements Serializable {

    private static final long serialVersionUID = 961267717148831831L;

    private final String key;

    private String content;

    public ConsoleLayoutInfo(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = FormLayoutInfoUtils.defaultConsoleLayoutInfoIfEmpty(content, new AnyTypeRestClient().list());
    }
}
