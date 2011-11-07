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
package org.syncope.core.persistence.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.Notification;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.EntityViolationType;

@Transactional
public class NotificationTest extends AbstractTest {

    @Autowired
    private NotificationDAO notificationDAO;

    @Test
    public void find() {
        Notification notification = notificationDAO.find(100L);
        assertNotNull(notification);
        assertNotNull(notification.getEvents());
        assertFalse(notification.getEvents().isEmpty());
        assertNotNull(notification.getAbout());
        assertTrue(notification.getAbout().checkValidity());
        assertNotNull(notification.getRecipients());
        assertTrue(notification.getRecipients().checkValidity());
    }

    @Test
    public final void findAll() {
        List<Notification> notifications = notificationDAO.findAll();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
    }

    @Test
    public void save() {
        Notification notification = new Notification();
        notification.addEvent("save");

        AttributeCond fullnameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttributeCond fullnameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        NodeCond about = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        notification.setAbout(about);

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        NodeCond recipients = NodeCond.getLeafCond(membCond);

        notification.setRecipients(recipients);

        notification.setSender("syncope@syncope-idm.org");
        notification.setSubject("Test notification");
        notification.setTemplate("test");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);
        assertNotNull(actual.getId());
    }

    @Test
    public void saveWithException() {
        Notification notification = new Notification();
        notification.addEvent("saveWithException");

        MembershipCond membCond = new MembershipCond();
        NodeCond about = NodeCond.getLeafCond(membCond);

        notification.setAbout(about);

        NodeCond recipients = NodeCond.getLeafCond(membCond);

        notification.setRecipients(recipients);

        notification.setSender("syncope@syncope-idm.org");
        notification.setSubject("Test notification");
        notification.setTemplate("test");


        Map<Class, Set<EntityViolationType>> violations;
        try {
            notificationDAO.save(notification);
            violations = null;
        } catch (InvalidEntityException e) {
            violations = e.getViolations();
        }

        assertNotNull(violations);
        assertEquals(1, violations.size());
        assertFalse(violations.get(Notification.class).isEmpty());
    }

    @Test
    public void delete() {
        notificationDAO.delete(100L);
        assertNull(notificationDAO.find(100L));
    }
}
