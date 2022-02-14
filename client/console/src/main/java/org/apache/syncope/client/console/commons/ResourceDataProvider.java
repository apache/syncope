package org.apache.syncope.client.console.commons;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceDataProvider extends DirectoryDataProvider<Serializable> {

    private static final long serialVersionUID = 3189980210236051840L;

    protected static final Logger LOG = LoggerFactory.getLogger(ResourceDataProvider.class);

    private final PageReference pageRef;

    protected int currentPage;

    private String keyword;

    private final ResourceRestClient restClient = new ResourceRestClient();

    public ResourceDataProvider(
            final int paginatorRows,
            final PageReference pageRef,
            final String keyword) {
        super(paginatorRows);
        setSort("keySortParam", SortOrder.ASCENDING);
        this.pageRef = pageRef;
        this.keyword = keyword;
    }

    @Override
    public Iterator<ResourceTO> iterator(final long first, final long count) {
        List<ResourceTO> result = Collections.emptyList();

        try {
            currentPage = ((int) first / paginatorRows);
            if (currentPage < 0) {
                currentPage = 0;
            }
            if (keyword == null || keyword.isEmpty()) {
                result = restClient.list();
            } else {
                result = restClient.list().stream().filter(resource ->
                        resource.getKey().toLowerCase().contains(keyword)).collect(Collectors.toList());
            }
        } catch (Exception e) {
            LOG.error("While searching", e);
            SyncopeConsoleSession.get().onException(e);

            Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            target.ifPresent(ajaxRequestTarget ->
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(ajaxRequestTarget));
        }

        SortParam<String> sortParam = getSort();
        if (sortParam != null) {
            result.sort(getComparator(sortParam));
        }

        return result.subList((int) first, (int) first + (int) count).iterator();
    }

    private Comparator<ResourceTO> getComparator(final SortParam<String> sortParam) {
        Comparator<ResourceTO> comparator;

        switch (sortParam.getProperty()) {
            case "keySortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getKey().toLowerCase()));
                break;
            case "connectorDisplayNameSortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getConnectorDisplayName().toLowerCase()));
                break;
            default:
                throw new IllegalStateException("The sort param " + sortParam.getProperty() + " is not correct");
        }

        if (!sortParam.isAscending()) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    @Override
    public long size() {
        long result = 0;

        try {
            if (keyword == null || keyword.isEmpty()) {
                result = restClient.list().size();
            } else {
                result = restClient.list().stream().filter(resource ->
                        resource.getKey().toLowerCase().contains(keyword)).count();
            }
        } catch (Exception e) {
            LOG.error("While requesting for size()", e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(target -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target));
        }

        return result;
    }

    @Override
    public IModel<Serializable> model(final Serializable object) {
        return new CompoundPropertyModel<>((ResourceTO) object);
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
