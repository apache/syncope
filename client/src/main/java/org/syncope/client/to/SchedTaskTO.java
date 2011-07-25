/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.to;

import java.util.Date;

public class SchedTaskTO extends TaskTO {

    private static final long serialVersionUID = -5722284116974636425L;

    private String cronExpression;

    private String jobClassName;

    private Date lastExec;

    private Date nextExec;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public Date getLastExec() {
        return new Date(lastExec.getTime());
    }

    public void setLastExec(Date lastExec) {
        this.lastExec = new Date(lastExec.getTime());
    }

    public Date getNextExec() {
        return new Date(nextExec.getTime());
    }

    public void setNextExec(Date nextExec) {
        this.nextExec = new Date(nextExec.getTime());
    }
}
