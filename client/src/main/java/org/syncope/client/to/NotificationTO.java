/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.search.NodeCond;

public class NotificationTO extends AbstractBaseBean {

    private Long id;

    private List<String> events;

    private NodeCond about;

    private NodeCond recipients;

    private String sender;

    private String subject;

    private String template;

    public NotificationTO() {
        events = new ArrayList<String>();
    }

    public NodeCond getAbout() {
        return about;
    }

    public void setAbout(NodeCond about) {
        this.about = about;
    }

    public List<String> getEvents() {
        return events;
    }

    public boolean addEvent(final String event) {
        return event != null && !events.contains(event) && events.add(event);
    }

    public boolean removeEvent(final String event) {
        return event != null && events.remove(event);
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NodeCond getRecipients() {
        return recipients;
    }

    public void setRecipients(NodeCond recipients) {
        this.recipients = recipients;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
