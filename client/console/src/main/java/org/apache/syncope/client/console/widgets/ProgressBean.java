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
package org.apache.syncope.client.console.widgets;

import java.io.Serializable;

public class ProgressBean implements Serializable {

    private static final long serialVersionUID = 7135463859007124458L;

    private String text;

    private int fraction;

    private int total;

    private String cssClass;

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public int getFraction() {
        return fraction;
    }

    public void setFraction(final int fraction) {
        this.fraction = fraction;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        this.total = total;
    }

    public float getPercent() {
        return getTotal() == 0 ? 0 : 100 * getFraction() / getTotal();
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(final String cssClass) {
        this.cssClass = cssClass;
    }

}
