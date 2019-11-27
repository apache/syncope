package org.apache.syncope.client.console.audit;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.to.AnyTO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class AuditEntryBean implements Serializable {
    private static final long serialVersionUID = -1207260204921071129L;

    private final String key;

    private String loggerName;

    private List<String> inputs;

    private String who;

    private String subCategory;

    private String event;

    private String result;

    private String before;

    private String output;

    private Date date;

    private String throwable;

    public AuditEntryBean(final AnyTO any) {
        this.key = any.getKey();
    }

    public String getKey() {
        return key;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(final List<String> inputs) {
        this.inputs = inputs;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
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

    public String getBefore() {
        return before;
    }

    public void setBefore(final String before) {
        this.before = before;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(final String output) {
        this.output = output;
    }

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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
            append(key).
            build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AuditEntryBean other = (AuditEntryBean) obj;
        return new EqualsBuilder().
            append(key, other.key).
            build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
            append(key).
            build();
    }
}
