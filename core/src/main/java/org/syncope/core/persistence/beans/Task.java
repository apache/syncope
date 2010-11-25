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
package org.syncope.core.persistence.beans;

import com.thoughtworks.xstream.XStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Cascade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;

/**
 * Encapsulate all information about a propagation task.
 */
@Entity
public class Task extends AbstractBaseBean {

    /**
     * Id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * @see PropagationMode
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropagationMode propagationMode;

    /**
     * @see ResourceOperationType
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceOperationType resourceOperationType;

    /**
     * The accountId on the target resource.
     */
    private String accountId;

    /**
     * The (optional) former accountId on the target resource.
     */
    private String oldAccountId;

    /**
     * Attributes to be propagated.
     */
    @Lob
    @Column(nullable = false)
    private String xmlAttributes;

    /**
     * TargetResource to which the propagation happens.
     */
    @ManyToOne
    private TargetResource resource;

    /**
     * When this task has been (or will be) executed, what its result was.
     */
    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "task",
    fetch = FetchType.EAGER)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private List<TaskExecution> executions;

    /**
     * Default constructor.
     */
    public Task() {
        super();

        executions = new ArrayList<TaskExecution>();
    }

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public Set<Attribute> getAttributes() {
        Set<Attribute> result = Collections.EMPTY_SET;

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = (XStream) context.getBean("xStream");
        try {
            result = (Set<Attribute>) xStream.fromXML(
                    URLDecoder.decode(xmlAttributes, "UTF-8"));
        } catch (Throwable t) {
            LOG.error("During attribute deserialization", t);
        }

        return result;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = (XStream) context.getBean("xStream");
        try {
            xmlAttributes = URLEncoder.encode(
                    xStream.toXML(attributes), "UTF-8");
        } catch (Throwable t) {
            LOG.error("During attribute serialization", t);
        }
    }

    public boolean addExecution(TaskExecution execution) {
        return executions.add(execution);
    }

    public boolean removeExecution(TaskExecution execution) {
        return executions.remove(execution);
    }

    public List<TaskExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<TaskExecution> executions) {
        this.executions.clear();
        if (executions != null && !executions.isEmpty()) {
            this.executions.addAll(executions);
        }
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public ResourceOperationType getResourceOperationType() {
        return resourceOperationType;
    }

    public void setResourceOperationType(
            final ResourceOperationType resourceOperationType) {

        this.resourceOperationType = resourceOperationType;
    }

    public TargetResource getResource() {
        return resource;
    }

    public void setResource(TargetResource resource) {
        this.resource = resource;
    }
}
