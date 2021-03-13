package com.redhat.labs.lodestar.resource.engagement;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SingleFilterOptions;
import com.redhat.labs.lodestar.resource.BackendResource;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GetEngagementResource extends BackendResource {

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given customer and project names.")
    @Counted(name = "engagement-get-counted")
    @Timed(name = "enagement-get-timer", unit = MetricUnits.MILLISECONDS)
    public Response get(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @BeanParam SingleFilterOptions filterOptions) {

        filterOptions.validateOptions();

        Engagement engagement = getEngagementService().getByCustomerAndProjectName(customerName, projectName,
                filterOptions);
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "A list or empty list of engagement resources returned") })
    @Operation(summary = "Returns all engagement resources from the database.  Can be empty list if none found.")
    @Counted(name = "engagement-get-all-counted")
    @Timed(name = "engagement-get-all-timer", unit = MetricUnits.MILLISECONDS)
    public List<Engagement> getAll(@QueryParam("categories") String categories,
            @BeanParam ListFilterOptions filterOptions) {

        // set suggest option if set
        filterOptions.setSuggestion(categories);
        return getEngagementService().getAll(filterOptions);

    }

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with id does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given id.")
    @Counted(name = "engagement-get-by-uuid-counted")
    @Timed(name = "engagement-get-by-uuid-timer", unit = MetricUnits.MILLISECONDS)
    public Response get(@PathParam("id") String uuid, @BeanParam SingleFilterOptions filterOptions) {

        filterOptions.validateOptions();
        Engagement engagement = getEngagementService().getByUuid(uuid, filterOptions);
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

}
