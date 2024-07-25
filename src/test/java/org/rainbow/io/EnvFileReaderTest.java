package org.rainbow.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.rainbow.io.EnvFileReader.ENV_FILE_EMPTY;
import static org.rainbow.io.EnvFileReader.ENV_FILE_NOT_SET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.rainbow.environment.Env;

class EnvFileReaderTest {

    private static final String ENCRYPT_KEY_FILE = "ENCRYPT_KEY_FILE";

    @Test
    void read_EnvFileIsNotSetAndRequiredIsTrue_ThrowIllegalStateException() {
        try (var envMockedStatic = mockStatic(Env.class)) {
            // Emulate unset environment variable by returning null
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(null);

            var e = assertThrows(IllegalStateException.class, () -> EnvFileReader.read(ENCRYPT_KEY_FILE, true));

            assertEquals(String.format(ENV_FILE_NOT_SET, ENCRYPT_KEY_FILE), e.getMessage());
        }
    }

    @Test
    void read_EnvFileOnlyContainsBlankLines_ThrowIllegalStateException() throws IOException {
        var file = createTempFile();
        var strings = new ArrayList<String>();
        // Add only blank lines
        strings.add(StringUtils.EMPTY);
        strings.add(System.lineSeparator());
        strings.add("    ");

        write(file, strings);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var e = assertThrows(IllegalStateException.class, () -> EnvFileReader.read(ENCRYPT_KEY_FILE, true));

            assertEquals(String.format(ENV_FILE_EMPTY, file.getPath()), e.getMessage());
        }
    }

    @Test
    void read_EnvFileIsNotSetAndRequiredIsFalse_ReturnEmptyString() throws IOException {
        try (var envMockedStatic = mockStatic(Env.class)) {
            // Emulate unset environment variable by returning null
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(null);

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, false);

            assertEquals(StringUtils.EMPTY, content);
        }
    }

    @Test
    void read_EnvFileIsNotEmpty_ReturnContent() throws IOException {
        var file = createTempFile();
        var strings = generateRandomStrings(2);

        write(file, strings);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, true);

            assertEquals(join(strings), content);
        }
    }

    @Test
    void read_FirstLineIsBlankAndSecondLineIsNotBlank_ReturnContent() throws IOException {
        var file = createTempFile();
        var strings = generateRandomStrings(1);
        // Make the first line blank
        strings.add(0, StringUtils.EMPTY);

        write(file, strings);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, true);

            assertEquals(join(strings), content);
        }
    }

    @Test
    void read_EnvFileIsEmptyAndRequiredIsTrue_ThrowIllegalStateException() throws IOException {
        var file = createTempFile();

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var e = assertThrows(IllegalStateException.class, () -> EnvFileReader.read(ENCRYPT_KEY_FILE, true));

            assertEquals(String.format(ENV_FILE_EMPTY, file.getPath()), e.getMessage());
        }
    }

    @Test
    void read_EnvFileIsEmptyAndRequiredIsFalse_ReturnEmptyString() throws IOException {
        var file = createTempFile();

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, false);

            assertEquals(StringUtils.EMPTY, content);
        }
    }

    @Test
    void read_OnlyEnvNameIsGiven_ReturnContent() throws IOException {
        var file = createTempFile();
        var lines = generateRandomStrings(2);

        write(file, lines);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE);

            assertEquals(join(lines), content);
        }
    }

    private File createTempFile() throws IOException {
        var file = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tmp");
        file.deleteOnExit();
        return file;
    }

    private void write(File file, List<String> strings) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(file, true))) {
            for (var string : strings) {
                writer.append(string);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private List<String> generateRandomStrings(int count) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            strings.add(RandomStringUtils.random(10, true, true));
        }
        return strings;
    }

    private String join(List<String> lines) {
        return lines.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
