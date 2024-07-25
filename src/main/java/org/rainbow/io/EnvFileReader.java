package org.rainbow.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.rainbow.environment.Env;

public final class EnvFileReader {

    // Fields made package-private for unit testing
    static final String ENV_FILE_NOT_SET = "An environment variable with name '%s' must be set to a non-blank value";
    static final String ENV_FILE_EMPTY = "The file with path '%s' must contain a non-blank line";

    private EnvFileReader() {
    }

    public static String read(String envName) throws IOException {
        return read(envName, true);
    }

    public static String read(String envName, boolean required) throws IOException {
        String fileName = Env.get(envName);

        if (StringUtils.isBlank(fileName)) {
            if (required) {
                throw new IllegalStateException(String.format(ENV_FILE_NOT_SET, envName));
            }
            return StringUtils.EMPTY;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            if (StringUtils.isNotBlank(content)) {
                return content;
            }
            if (required) {
                throw new IllegalStateException(String.format(ENV_FILE_EMPTY, fileName));
            }
            return StringUtils.EMPTY;
        }
    }

}
