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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        var lines = new ArrayList<String>();
        // Add only blank lines
        lines.add(StringUtils.EMPTY);
        lines.add(System.lineSeparator());
        lines.add("    ");

        write(file, lines);

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
        var lines = generateRandomStrings(2);

        write(file, lines);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, true);

            assertEquals(join(lines), content);
        }
    }

    @Test
    void read_FirstLineIsBlankAndSecondLineIsNotBlank_ReturnContent() throws IOException {
        var file = createTempFile();
        var lines = generateRandomStrings(1);
        // Make the first line blank
        lines.add(0, StringUtils.EMPTY);

        write(file, lines);

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(ENCRYPT_KEY_FILE)).thenReturn(file.getPath());

            var content = EnvFileReader.read(ENCRYPT_KEY_FILE, true);

            assertEquals(join(lines), content);
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

    @Test
    void readAndSet_ByEnvNamesByPropAreGiven_ReadEnvFileAndSetSystemProperties() throws IOException {
        var envName1File = "ENV_NAME_1_FILE";
        var envName2File = "ENV_NAME_2_FILE";
        var prop1 = "prop1";
        var prop2 = "prop2";

        var envNamesByProp = Map.of(envName1File, prop1, envName2File, prop2);

        var file1 = createTempFile();
        var value1 = RandomStringUtils.random(10);
        write(file1, Arrays.asList(value1));

        var file2 = createTempFile();
        var value2 = RandomStringUtils.random(10);
        write(file2, Arrays.asList(value2));

        try (var envMockedStatic = mockStatic(Env.class)) {
            envMockedStatic.when(() -> Env.get(envName1File)).thenReturn(file1.getPath());
            envMockedStatic.when(() -> Env.get(envName2File)).thenReturn(file2.getPath());

            EnvFileReader.readAndSet(envNamesByProp);

            assertEquals(value1, System.getProperty(prop1));
            assertEquals(value2, System.getProperty(prop2));
        }
    }

    private File createTempFile() throws IOException {
        var file = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tmp");
        file.deleteOnExit();
        return file;
    }

    private void write(File file, List<String> lines) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(file, true))) {
            for (var line : lines) {
                writer.append(line);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private List<String> generateRandomStrings(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> RandomStringUtils.random(10, true, true))
                .collect(Collectors.toList());
    }

    private String join(List<String> lines) {
        return lines.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
