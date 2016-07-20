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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

public class TextInfo extends TagInfo {

    private String display;
    private String text;
    private int position;

    public TextInfo(final String text, final int position) {
        this(text, text, position);
    }

    public TextInfo(final String display, final String text, final int position) {
        super(null, false);
        this.display = display;
        this.text = text;
        this.position = position;
    }

    public String getDisplayString() {
        return this.display;
    }

    public String getText() {
        return this.text;
    }

    public int getPosition() {
        return this.position;
    }

}
