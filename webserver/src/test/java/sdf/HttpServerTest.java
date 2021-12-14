package sdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

public class HttpServerTest {

    @Test
    public void checkDocPaths_DocPathsExist_ShouldReturnTrue() throws IOException {
        String testFolderName = "testFolder";
        String testFileName = "test.html";
        Path testDocRoot = Paths.get(testFolderName);
        File directory = testDocRoot.toFile();
        if (directory.exists()) {
            directory.delete();
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path testFilePath = Paths.get(testFolderName, testFileName);
        Files.write(testFilePath, List.of("test"));


        HttpServer server = new HttpServer(3001, testFolderName);
        assertTrue(server.checkDocPaths());
    }

    @Test
    public void checkDocPaths_DocPathsDoesNotExist_ShouldReturnFalse() throws IOException {
        String testFolderName = "testFolder";
        String testAltFolderName = "testFolderAlt";
        String testFileName = "test.html";
        Path testDocRoot = Paths.get(testFolderName);
        Path testAltDocRoot = Paths.get(testAltFolderName);
        File directory = testDocRoot.toFile();
        File directoryAlt = testAltDocRoot.toFile();
        if (directory.exists()) {
            directory.delete();
        }
        if (directoryAlt.exists()) {
            directoryAlt.delete();
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path testFilePath = Paths.get(testFolderName, testFileName);
        Files.write(testFilePath, List.of("test"));


        HttpServer server = new HttpServer(3001, testFolderName + ":" + testAltFolderName);
        assertFalse(server.checkDocPaths());
    }
}
