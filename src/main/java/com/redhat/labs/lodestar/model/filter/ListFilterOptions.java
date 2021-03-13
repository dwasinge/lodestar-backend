package com.redhat.labs.lodestar.model.filter;

import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class ListFilterOptions extends SingleFilterOptions {

    @Setter
    @Parameter(name = "suggestion", required = false, description = "case insensitive search string")
    @QueryParam("suggestion")
    private String suggestion;

    @Parameter(name = "sortOrder", required = false, description = "response list sort order.  valid values are 'ASC' or 'DESC'")
    @QueryParam("sortOrder")
    private SortOrder sortOrder;

    @Parameter(name = "limit", required = false, description = "result list will be limited to the given number.  ignored if page supplied")
    @QueryParam("limit")
    private Integer limit;

    @Parameter(name = "page", required = false, description = "page number of results to return")
    @QueryParam("page")
    private Integer page;

    @Parameter(name = "perPage", required = false, description = "number of results per page to return")
    @QueryParam("perPage")
    private Integer perPage;

    public Optional<String> getSuggestion() {
        return Optional.ofNullable(suggestion);
    }
    
    public Optional<SortOrder> getSortOrder() {
        return Optional.ofNullable(sortOrder);
    }

    public Optional<Integer> getLimit() {
        return Optional.ofNullable(limit);
    }

    public Optional<Integer> getPage() {
        return Optional.ofNullable(page);
    }

    public Optional<Integer> getPerPage() {
        return Optional.ofNullable(perPage);
    }

}
