package com.redhat.labs.lodestar.model.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SingleFilterOptions {

    @Parameter(name = "include", required = false, description = "comma separated list of field names to include in response")
    @QueryParam("include")
    private String include;

    @Parameter(name = "exclude", required = false, description = "comma separated list of field names to exclude in response")
    @QueryParam("exclude")
    private String exclude;

    public void validateOptions() {
        if(null != include && null != exclude) {
            throw new WebApplicationException("cannot provide both include and exclude parameters", 400);
        }
    }

    public Optional<Set<String>> getIncludeList() {

        Optional<Set<String>> includeOptional = createSet(include);

        if (includeOptional.isEmpty() || null == exclude) {
            return includeOptional;
        }

        // get exlude set
        Set<String> excludeSet = getExcludeList().orElse(Set.of());

        // filter any includes also in exclude
        return Optional.of(includeOptional.get().stream().filter(attribute -> !excludeSet.contains(attribute))
                .collect(Collectors.toSet()));

    }

    public Optional<Set<String>> getExcludeList() {
        return createSet(exclude);
    }

    private Optional<Set<String>> createSet(String value) {

        if (null == value) {
            return Optional.empty();
        }

        return Optional.of(parseAttributes(value));

    }

    private Set<String> parseAttributes(String value) {

        if (null == value || value.isEmpty()) {
            return new HashSet<>();
        }

        return Stream.of(value.split(",")).map(this::snakeToCamelCase).collect(Collectors.toSet());

    }

    private String snakeToCamelCase(String value) {

        // split lowercase value based on underscore
        List<String> tokens = Stream.of(value.toLowerCase().split("_")).collect(Collectors.toList());

        // start string with first lower case token
        StringBuilder builder = new StringBuilder(tokens.remove(0));

        // capitalize first letter of each remaining token
        tokens.stream().forEach(token -> {
            String tmp = (1 == token.length()) ? token.toUpperCase()
                    : token.substring(0, 1).toUpperCase() + token.substring(1);
            builder.append(tmp);
        });

        return builder.toString();

    }

}
