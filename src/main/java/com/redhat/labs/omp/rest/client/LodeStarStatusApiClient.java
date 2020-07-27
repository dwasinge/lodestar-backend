package com.redhat.labs.omp.rest.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.VersionDetailSummary;

@RegisterRestClient(configKey = "lodestar.status.api")
public interface LodeStarStatusApiClient {

    @GET
    @Produces("application/json")
    @Path("/api/v1/version/manifest")
    public VersionDetailSummary getVersionDetailSummary();

}
