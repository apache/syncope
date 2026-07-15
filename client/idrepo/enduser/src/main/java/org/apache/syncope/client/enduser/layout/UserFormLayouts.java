package org.apache.syncope.client.enduser.layout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;

public class UserFormLayouts implements Serializable {

    private static final long serialVersionUID = 9106933641699158419L;

    private final Map<String, UserFormLayoutInfo> layouts;

    public UserFormLayouts() {
        this(null);
    }

    @JsonCreator
    public UserFormLayouts(@JsonProperty("layouts") final Map<String, UserFormLayoutInfo> layouts) {
        this.layouts = layouts == null ? new HashMap<>() : new HashMap<>(layouts);
        this.layouts.putIfAbsent(SyncopeConstants.ROOT_REALM, new UserFormLayoutInfo());
    }

    public Map<String, UserFormLayoutInfo> getLayouts() {
        return layouts;
    }

    public UserFormLayoutInfo getLayout(final String realm) {
        if (StringUtils.isNotBlank(realm)) {
            String current = realm;

            while (current != null) {
                UserFormLayoutInfo layout = layouts.get(current);
                if (layout != null) {
                    return layout;
                }

                int lastSlash = current.lastIndexOf('/');
                current = lastSlash <= 0 ? null : current.substring(0, lastSlash);
            }
        }

        return layouts.get(SyncopeConstants.ROOT_REALM);
    }
}
