package com.redhat.labs.lodestar.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

@Readiness
@ApplicationScoped
public class GitApiHealthCheck implements HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitApiHealthCheck.class);
    private static final String NAME = "Git API";

    @Inject
    @RestClient
    LodeStarGitLabAPIService service;

    @Override
    public HealthCheckResponse call() {

        try {
            service.getVersion();
            return HealthCheckResponse.up(NAME);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            String body = e.getResponse().getEntity().toString();
            LOGGER.warn("git api health check failed...{}:{}:{}", status, body, e.getMessage());
            return HealthCheckResponse.down(NAME);
        }

    }

}
