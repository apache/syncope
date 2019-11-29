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
package org.apache.syncope.common.lib.to;

import org.apache.syncope.common.lib.BaseBean;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "audit")
@XmlType
public class AuditEntryTO extends BaseBean implements EntityTO  {
    private static final long serialVersionUID = 1215115961911228005L;

    private final List<String> inputs = new ArrayList<>();

    private String who;

    private String subCategory;

    private String event;

    private String result;

    private Object before;

    private Object output;

    private Date date;

    private String throwable;

    private String key;

    private String loggerName;

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(final String throwable) {
        this.throwable = throwable;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(final Object output) {
        this.output = output;
    }

    public Object getBefore() {
        return before;
    }

    public void setBefore(final Object before) {
        this.before = before;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(final String subCategory) {
        this.subCategory = subCategory;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(final String event) {
        this.event = event;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }
}
