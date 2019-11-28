package org.apache.syncope.client.console.rest;

import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import java.util.List;

public class AuditHistoryRestClient extends BaseRestClient {

    private static final long serialVersionUID = -381814125643246243L;

    public List<AuditEntryTO> search(final String key,
                                     final int page,
                                     final int size,
                                     final SortParam<String> sort,
                                     final List<String> events,
                                     final List<AuditElements.Result> results) {
        AuditQuery query = new AuditQuery.Builder()
            .size(size)
            .key(key)
            .page(page)
            .events(events)
            .results(results)
            .orderBy(toOrderBy(sort))
            .build();
        return getService(AuditService.class).search(query).getResult();
    }

    public int count(final String key) {
        AuditQuery query = new AuditQuery.Builder()
            .key(key)
            .build();
        return getService(AuditService.class).search(query).getTotalCount();
    }

    public int count(final String key,
                     final List<String> events,
                     final List<AuditElements.Result> results) {
        AuditQuery query = new AuditQuery.Builder()
            .key(key)
            .events(events)
            .results(results)
            .build();
        return getService(AuditService.class).search(query).getTotalCount();
    }

}

