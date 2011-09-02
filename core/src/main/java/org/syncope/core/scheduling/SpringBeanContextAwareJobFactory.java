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
package org.syncope.core.scheduling;

import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * From blog post at http://sloanseaman.com/wordpress/?p=120.
 */
public class SpringBeanContextAwareJobFactory extends SpringBeanJobFactory {

    /**
     * Properties to ignore.
     */
    private String[] ignoredUnknownProperties;

    /**
     * Scheduler context.
     */
    private SchedulerContext schedulerContext;

    @Override
    public void setIgnoredUnknownProperties(
            final String[] ignoredUnknownProperties) {

        super.setIgnoredUnknownProperties(ignoredUnknownProperties);
        if (ignoredUnknownProperties != null) {
            this.ignoredUnknownProperties = ignoredUnknownProperties.clone();
        }
    }

    @Override
    public void setSchedulerContext(final SchedulerContext schedulerContext) {
        super.setSchedulerContext(schedulerContext);
        this.schedulerContext = schedulerContext;
    }

    /**
     * An implementation of SpringBeanJobFactory that retrieves the bean from
     * the Spring context so that autowiring and transactions work
     *
     * This method is overriden.
     * @see org.springframework.scheduling.quartz.SpringBeanJobFactory
     * #createJobInstance(org.quartz.spi.TriggerFiredBundle)
     *
     * @param bundle Spring's TriggerFiredBundle
     * @return the actual JobInstance
     * @throws Exception if anything goes wrong
     */
    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle)
            throws Exception {

        XmlWebApplicationContext ctx =
                (XmlWebApplicationContext) schedulerContext.get(
                "applicationContext");
        Object job = ctx.getBean(bundle.getJobDetail().getName());
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
        if (isEligibleForPropertyPopulation(bw.getWrappedInstance())) {
            MutablePropertyValues pvs = new MutablePropertyValues();
            if (this.schedulerContext != null) {
                pvs.addPropertyValues(this.schedulerContext);
            }
            pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
            pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
            if (this.ignoredUnknownProperties != null) {
                for (String propName : this.ignoredUnknownProperties) {
                    if (pvs.contains(propName)
                            && !bw.isWritableProperty(propName)) {

                        pvs.removePropertyValue(propName);
                    }
                }
                bw.setPropertyValues(pvs);
            } else {
                bw.setPropertyValues(pvs, true);
            }
        }
        return job;
    }
}
