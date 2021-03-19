package com.redhat.labs.lodestar.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassFieldUtils {

    private ClassFieldUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String snakeToCamelCase(String value) {

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
