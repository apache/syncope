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
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.validation.constraints.NotNull;
import org.syncope.types.TraceLevel;

@Entity
public class NotificationTask extends Task {

    private static final long serialVersionUID = 95731573485279180L;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> recipients;

    @NotNull
    private String sender;

    @NotNull
    private String subject;

    @NotNull
    private String textBody;

    @NotNull
    private String htmlBody;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel traceLevel;

    public NotificationTask() {
        super();

        recipients = new HashSet<String>();
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public boolean addRecipient(final String recipient) {
        return recipients.add(recipient);
    }

    public boolean removeRecipient(final String recipient) {
        return recipients.remove(recipient);
    }

    public void setRecipients(final Set<String> recipients) {
        this.recipients.clear();
        if (recipients != null) {
            this.recipients.addAll(recipients);
        }
    }

    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(final String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(final String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }
}
