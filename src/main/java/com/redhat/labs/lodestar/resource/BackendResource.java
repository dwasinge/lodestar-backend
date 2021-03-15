package com.redhat.labs.lodestar.resource;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.labs.lodestar.service.EngagementService;

public abstract class BackendResource {

    private static final String NAME_CLAIM = "name";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String USER_EMAIL_CLAIM = "email";

    public static final String DEFAULT_USERNAME = "lodestar-user";
    public static final String DEFAULT_EMAIL = "lodestar-email";

    public static final String ACCESS_CONTROL_EXPOSE_HEADER = "Access-Control-Expose-Headers";
    public static final String LAST_UPDATE_HEADER = "last-update";

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    public String getUsernameFromToken() {

        // Use `name` claim first
        Optional<String> optional = claimIsValid(NAME_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        // use `preferred_username` claim if `name` not valid
        optional = claimIsValid(PREFERRED_USERNAME_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        // use `email` if username not valid
        return getUserEmailFromToken();

    }

    public String getUserEmailFromToken() {

        Optional<String> optional = claimIsValid(USER_EMAIL_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        return DEFAULT_EMAIL;

    }

    Optional<String> claimIsValid(String claimName) {

        // get claim by name
        Optional<String> optional = jwt.claim(claimName);

        // return if no value found
        if (!optional.isPresent()) {
            return optional;
        }

        String value = optional.get();

        // return empty optional if value is whitespace
        if (value.trim().equals("")) {
            return Optional.empty();
        }

        // valid return
        return optional;

    }

}
