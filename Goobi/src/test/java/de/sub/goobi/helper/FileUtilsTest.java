package de.sub.goobi.helper;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.sub.goobi.AbstractTest;
import de.sub.goobi.config.ConfigProjectsTest;
import de.sub.goobi.config.ConfigurationHelper;

public class FileUtilsTest extends AbstractTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path currentFolder;

    @Before
    public void setUp() throws IOException, URISyntaxException {

        Path template = Paths.get(ConfigProjectsTest.class.getClassLoader().getResource(".").getFile());
        Path goobiFolder = Paths.get(template.getParent().getParent().toString() + "/src/test/resources/config/goobi_config.properties"); // for junit tests in eclipse
        if (!Files.exists(goobiFolder)) {
            goobiFolder = Paths.get("target/test-classes/config/goobi_config.properties"); // to run mvn test from cli or in jenkins
        }
        ConfigurationHelper.resetConfigurationFile();
        ConfigurationHelper.getInstance().setParameter("goobiFolder", goobiFolder.getParent().getParent().toString()+ "/");

        currentFolder = folder.newFolder("temp").toPath();
        Path tif = Paths.get(currentFolder.toString(), "00000001.tif");
        Files.createFile(tif);
    }

    @Test
    public void testGetNumberOfFiles() throws IOException {

        Path notExistingFile = Paths.get("somewhere");
        long value = StorageProvider.getInstance().getNumberOfFiles(notExistingFile);
        assertEquals(0, value);

        Path file = Paths.get(ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_JunitImportPluginError.xml");
        value = StorageProvider.getInstance().getNumberOfFiles(file);
        assertEquals(0, value);

        value = StorageProvider.getInstance().getNumberOfFiles(currentFolder);
        assertEquals(1, value);

    }

    @Test
    public void testGetNumberOfFilesString() throws IOException {

        String folderName = currentFolder.toString();
        int value = StorageProvider.getInstance().getNumberOfFiles(folderName);
        assertEquals(1, value);

    }
}
