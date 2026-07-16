package org.apache.syncope.client.enduser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.layout.UserFormLayouts;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.junit.jupiter.api.Test;

class UserFormLayoutsTest {

    @Test
    void shouldReturnExactRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even"));
    }

    @Test
    void shouldReturnParentRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even/two"));
    }

    @Test
    void shouldReturnRootRealmLayoutWhenNoParentExists() {
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of());
        UserFormLayoutInfo rootLayout = userFormLayouts.getLayouts().get(SyncopeConstants.ROOT_REALM);
        assertNotNull(rootLayout);
        assertSame(rootLayout, userFormLayouts.getLayout("/odd"));
    }

    @Test
    void shouldReturnCustomRootRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of(SyncopeConstants.ROOT_REALM, customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/odd"));
    }

    @Test
    void shouldNotLoopWhenRealmDoesNotExist() {
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of());
        assertNotNull(userFormLayouts.getLayout("/unknown"));
    }

    @Test
    void shouldReturnCustomLayoutForRealm() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even"));
    }

    @Test
    void shouldKeepCustomRootRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of(SyncopeConstants.ROOT_REALM, customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/"));
    }
}
