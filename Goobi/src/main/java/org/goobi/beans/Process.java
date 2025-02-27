package org.goobi.beans;

import java.io.FileOutputStream;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi-workflow
 * 			- http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.goobi.io.BackupFileManager;
import org.goobi.io.FileListFilter;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.LogType;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.GoobiScript;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.UghHelper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.helper.tasks.ProcessSwapInTask;
import de.sub.goobi.metadaten.Image;
import de.sub.goobi.metadaten.ImageCommentHelper;
import de.sub.goobi.metadaten.MetadatenHelper;
import de.sub.goobi.metadaten.MetadatenSperrung;
import de.sub.goobi.persistence.managers.DocketManager;
import de.sub.goobi.persistence.managers.MasterpieceManager;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import de.sub.goobi.persistence.managers.StepManager;
import de.sub.goobi.persistence.managers.TemplateManager;
import de.sub.goobi.persistence.managers.UserManager;
import io.goobi.workflow.xslt.XsltPreparatorDocket;
import io.goobi.workflow.xslt.XsltPreparatorMetadata;
import io.goobi.workflow.xslt.XsltToPdf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;

@Log4j2
public class Process implements Serializable, DatabaseObject, Comparable<Process> {
    private static final long serialVersionUID = -6503348094655786275L;
    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private String titel;
    @Getter
    @Setter
    private String ausgabename;
    private Boolean istTemplate;
    private Boolean inAuswahllisteAnzeigen;
    @Setter
    private Project projekt;
    @Getter
    @Setter
    private Date erstellungsdatum;
    @Setter
    private List<Step> schritte;
    //    private List<HistoryEvent> history;
    @Setter
    private List<Masterpiece> werkstuecke;
    @Setter
    private List<Template> vorlagen;
    @Setter
    private List<Processproperty> eigenschaften;
    @Getter
    @Setter
    private String sortHelperStatus;
    @Setter
    private Integer sortHelperImages;
    @Setter
    private Integer sortHelperArticles;
    @Setter
    private Integer sortHelperMetadata;
    @Setter
    private Integer sortHelperDocstructs;
    @Getter
    @Setter
    private Ruleset regelsatz;
    //    private Integer batchID;
    @Getter
    @Setter
    private Batch batch;
    private Boolean swappedOut = false;
    private Boolean panelAusgeklappt = false;
    private Boolean selected = false;
    @Setter
    private Docket docket;

    private String imagesTiffDirectory = null;
    private String imagesOrigDirectory = null;

    @Getter
    @Setter
    private List<LogEntry> processLog = new LinkedList<>();

    private BeanHelper bhelp = new BeanHelper();

    // temporär
    @Getter
    @Setter
    private Integer projectId;
    @Getter
    @Setter
    private Integer MetadatenKonfigurationID;
    @Getter
    @Setter
    private Integer docketId;

    private final MetadatenSperrung msp = new MetadatenSperrung();
    Helper help = new Helper();

    private HashMap<String, String> tempVariableMap = new HashMap<>();

    @Getter
    @Setter
    private String content = "";
    @Getter
    @Setter
    private String secondContent = "";
    @Getter
    @Setter
    private String thirdContent = "";

    @Getter
    @Setter
    private boolean mediaFolderExists = false;

    //    @Inject
    //    private LoginBean loginForm;

    private List<StringPair> metadataList = new ArrayList<>();
    private String representativeImage = null;

    private List<SelectItem> folderList = new ArrayList<>();
    @Getter
    @Setter
    private String currentFolder;

    @Getter
    @Setter
    private Part uploadedFile = null;
    @Getter
    @Setter
    private String uploadFolder = "intern";

    private Path tempFileToImport;
    private String basename;

    @Getter
    private boolean showFileDeletionButton;

    @Getter
    private boolean pauseAutomaticExecution;

    private static final Object xmlWriteLock = new Object();

    public Process() {
        this.swappedOut = false;
        this.titel = "";
        this.istTemplate = false;
        this.inAuswahllisteAnzeigen = false;
        this.eigenschaften = new ArrayList<>();
        this.schritte = new ArrayList<>();
        this.erstellungsdatum = new Date();

    }

    public void setIstTemplate(boolean istTemplate) {
        this.istTemplate = istTemplate;
    }

    public boolean isIstTemplate() {
        if (this.istTemplate == null) {
            this.istTemplate = Boolean.valueOf(false);
        }
        return this.istTemplate;
    }

    public List<Step> getSchritte() {
        if ((this.schritte == null || schritte.isEmpty()) && id != null) {
            schritte = StepManager.getStepsForProcess(id);
        }
        return this.schritte;
    }

    public boolean containsStepOfOrder(int order) {
        for (int i = 0; i < this.schritte.size(); i++) {
            if (this.schritte.get(i).getReihenfolge() == order) {
                return true;
            }
        }
        return false;
    }

    public boolean getContainsExportStep() {
        this.getSchritte();
        for (int i = 0; i < this.schritte.size(); i++) {
            if (this.schritte.get(i).isTypExportDMS() || this.schritte.get(i).isTypExportRus()) {
                return true;
            }
        }
        return false;
    }

    //    public List<HistoryEvent> getHistory() {

    //        if (this.history == null && id != null) {
    //            List<HistoryEvent> events = ProcessManager.getHistoryEvents(id);
    //            for (HistoryEvent he : events) {
    //                he.setProcess(this);
    //            }
    //            this.history = events;
    //        }
    //        return this.history;
    //    }

    //    public void setHistory(List<HistoryEvent> history) {
    //
    //        this.history = history;
    //    }

    public List<Template> getVorlagen() {
        if ((vorlagen == null || vorlagen.isEmpty()) && id != null) {
            vorlagen = TemplateManager.getTemplatesForProcess(id);
        }
        return this.vorlagen;
    }

    public List<Masterpiece> getWerkstuecke() {
        if ((werkstuecke == null || werkstuecke.isEmpty()) && id != null) {
            werkstuecke = MasterpieceManager.getMasterpiecesForProcess(id);
        }
        return this.werkstuecke;
    }

    public List<Processproperty> getEigenschaften() {
        if ((eigenschaften == null || eigenschaften.isEmpty()) && id != null) {
            eigenschaften = PropertyManager.getProcessPropertiesForProcess(id);
        }
        return this.eigenschaften;
    }

    /*
     * Metadaten-Sperrungen zurückgeben
     */

    public User getBenutzerGesperrt() {
        User rueckgabe = null;
        if (MetadatenSperrung.isLocked(this.id.intValue())) {
            String benutzerID = this.msp.getLockBenutzer(this.id.intValue());
            try {
                rueckgabe = UserManager.getUserById(Integer.valueOf(benutzerID));
            } catch (Exception e) {
                Helper.setFehlerMeldung(Helper.getTranslation("userNotFound"), e);
            }
        }
        return rueckgabe;
    }

    public long getMinutenGesperrt() {
        return this.msp.getLockSekunden(this.id) / 60;
    }

    public long getSekundenGesperrt() {
        return this.msp.getLockSekunden(this.id) % 60;
    }

    public String getImagesTifDirectory(boolean useFallBack) throws IOException, InterruptedException, SwapException, DAOException {
        if (this.imagesTiffDirectory != null && StorageProvider.getInstance().isDirectory(Paths.get(this.imagesTiffDirectory))) {
            return this.imagesTiffDirectory;
        }
        Path dir = Paths.get(getImagesDirectory());

        /* nur die _tif-Ordner anzeigen, die nicht mir orig_ anfangen */

        String mediaFolder = VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessImagesMainDirectoryName(), this);

        if (!StorageProvider.getInstance().isDirectory(Paths.get(dir.toString(), mediaFolder)) && useFallBack) {
            String configuredFallbackFolder = ConfigurationHelper.getInstance().getProcessImagesFallbackDirectoryName();
            if (StringUtils.isNotBlank(configuredFallbackFolder)) {
                String fallback = VariableReplacer.simpleReplace(configuredFallbackFolder, this);
                if (Files.exists(Paths.get(dir.toString(), fallback))) {
                    mediaFolder = fallback;
                }
            }
        }

        //if first fallback fails, fall back to largest thumbs folder if possible
        if (!StorageProvider.getInstance().isDirectory(Paths.get(dir.toString(), mediaFolder)) && useFallBack) {
            //fall back to largest thumbnail image
            java.nio.file.Path largestThumbnailDirectory = getThumbsDirectories(mediaFolder).entrySet()
                    .stream()
                    .sorted((entry1, entry2) -> entry2.getKey().compareTo(entry2.getKey()))
                    .map(Entry::getValue)
                    .map(string -> Paths.get(string))
                    .filter(StorageProvider.getInstance()::isDirectory)
                    .findFirst()
                    .orElse(null);
            if (largestThumbnailDirectory != null) {
                return largestThumbnailDirectory.toString();
            }
        }

        String rueckgabe = getImagesDirectory() + mediaFolder;
        if (!rueckgabe.endsWith(FileSystems.getDefault().getSeparator())) {
            rueckgabe += FileSystems.getDefault().getSeparator();
        }
        if (!ConfigurationHelper.getInstance().isUseMasterDirectory() && ConfigurationHelper.getInstance().isCreateMasterDirectory()) {
            FilesystemHelper.createDirectory(rueckgabe);
        }
        this.imagesTiffDirectory = rueckgabe;
        return rueckgabe;
    }

    /*
     * @return true if the Tif-Image-Directory exists, false if not
     */
    public Boolean getTifDirectoryExists() {
        return mediaFolderExists;
    }

    public Boolean getDisplayMETSButton() {
        if (sortHelperImages == null || sortHelperImages == 0) {
            if (ConfigurationHelper.getInstance().isMetsEditorValidateImages()) {
                return getTifDirectoryExists();
            }
        }
        return true;
    }

    public Boolean getDisplayPDFButton() {
        if (sortHelperImages == null || sortHelperImages == 0) {
            return getTifDirectoryExists();
        }
        return true;
    }

    public Boolean getDisplayDMSButton() {
        if (sortHelperDocstructs == null || sortHelperDocstructs == 0) {
            if (ConfigurationHelper.getInstance().isExportValidateImages()) {
                if (sortHelperImages == null || sortHelperImages == 0) {
                    return getTifDirectoryExists();
                }
            }
        }
        return true;
    }

    public String getImagesOrigDirectory(boolean useFallBack) throws IOException, InterruptedException, SwapException, DAOException {
        if (this.imagesOrigDirectory != null) {
            return this.imagesOrigDirectory;
        }
        if (ConfigurationHelper.getInstance().isUseMasterDirectory()) {
            Path dir = Paths.get(getImagesDirectory());

            /* nur die _tif-Ordner anzeigen, die mit orig_ anfangen */

            String masterFolder = VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessImagesMasterDirectoryName(), this);

            if (!StorageProvider.getInstance().isDirectory(Paths.get(dir.toString(), masterFolder)) && useFallBack) {
                String configuredFallbackFolder = ConfigurationHelper.getInstance().getProcessImagesFallbackDirectoryName();
                if (StringUtils.isNotBlank(configuredFallbackFolder)) {
                    String fallback = VariableReplacer.simpleReplace(configuredFallbackFolder, this);
                    if (Files.exists(Paths.get(dir.toString(), fallback))) {
                        masterFolder = fallback;
                    }
                }
            }

            //if first fallback fails, fall back to largest thumbs folder if possible
            if (!StorageProvider.getInstance().isDirectory(Paths.get(dir.toString(), masterFolder)) && useFallBack) {
                //fall back to largest thumbnail image
                java.nio.file.Path largestThumbnailDirectory = getThumbsDirectories(masterFolder).entrySet()
                        .stream()
                        .sorted((entry1, entry2) -> entry2.getKey().compareTo(entry2.getKey()))
                        .map(Entry::getValue)
                        .map(string -> Paths.get(string))
                        .filter(StorageProvider.getInstance()::isDirectory)
                        .findFirst()
                        .orElse(null);
                if (largestThumbnailDirectory != null) {
                    return largestThumbnailDirectory.toString();
                }
            }

            //            if (!origOrdner.equals("") && useFallBack) {
            //                String suffix = ConfigurationHelper.getInstance().getMetsEditorDefaultSuffix();
            //                if (!suffix.equals("")) {
            //                    Path tif = Paths.get(getImagesDirectory()).resolve(origOrdner);
            //                    List<String> files = StorageProvider.getInstance().list(tif.toString());
            //                    if (files == null || files.isEmpty()) {
            //                        List<String> folderList = StorageProvider.getInstance().list(dir.toString());
            //                        for (String folder : folderList) {
            //                            if (folder.endsWith(suffix)) {
            //                                origOrdner = folder;
            //                                break;
            //                            }
            //                        }
            //                    }
            //                }
            //            }

            String rueckgabe;
            if (!masterFolder.contains(FileSystems.getDefault().getSeparator())) {
                rueckgabe = getImagesDirectory() + masterFolder + FileSystems.getDefault().getSeparator();
            } else {
                rueckgabe = masterFolder;
            }
            if (ConfigurationHelper.getInstance().isUseMasterDirectory() && this.getSortHelperStatus() != "100000000"
                    && ConfigurationHelper.getInstance().isCreateMasterDirectory()) {
                FilesystemHelper.createDirectory(rueckgabe);
            }
            this.imagesOrigDirectory = rueckgabe;
            return rueckgabe;
        } else {
            return getImagesTifDirectory(useFallBack);
        }
    }

    /**
     * Get the image directory name matching the given thumbnail directory name. If the name ends with a number preceded by an underscore character,
     * then that part is removed from the returned name Otherwise, the whole name is returned
     * 
     * @param thumbDirName
     * @return
     */
    public String getMatchingImageDir(String thumbDirName) {
        if (thumbDirName.matches(".*_\\d+")) {
            return thumbDirName.substring(0, thumbDirName.lastIndexOf("_"));
        } else {
            return thumbDirName;
        }
    }

    public String getImagesDirectory() throws IOException, InterruptedException, SwapException, DAOException {
        String pfad = getProcessDataDirectory() + "images" + FileSystems.getDefault().getSeparator();
        FilesystemHelper.createDirectory(pfad);
        return pfad;
    }

    public String getSourceDirectory() throws IOException, InterruptedException, SwapException, DAOException {
        Path sourceFolder = Paths.get(getImagesDirectory(),
                VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessImagesSourceDirectoryName(), this));
        if (ConfigurationHelper.getInstance().isCreateSourceFolder() && !StorageProvider.getInstance().isDirectory(sourceFolder)) {
            StorageProvider.getInstance().createDirectories(sourceFolder);
        }
        return sourceFolder.toString();
    }

    public String getProcessDataDirectory() throws IOException, InterruptedException, SwapException, DAOException {
        String pfad = getProcessDataDirectoryIgnoreSwapping();

        if (isSwappedOutGui()) {
            ProcessSwapInTask pst = new ProcessSwapInTask();
            pst.initialize(this);
            pst.execute();
            if (pst.getStatusProgress() == -1) {
                if (!StorageProvider.getInstance().isFileExists(Paths.get(pfad, "images"))
                        && !StorageProvider.getInstance().isFileExists(Paths.get(pfad, "meta.xml"))) {
                    throw new SwapException(pst.getStatusMessage());
                } else {
                    setSwappedOutGui(false);
                }
                //				new ProzessDAO().save(this);
            }
        }
        return pfad;
    }

    public String getOcrDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getProcessDataDirectory() + "ocr" + FileSystems.getDefault().getSeparator();
    }

    public String getOcrTxtDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getOcrDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessOcrTxtDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    @Deprecated
    public String getOcrWcDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getOcrDirectory() + this.titel + "_wc" + FileSystems.getDefault().getSeparator();
    }

    public String getOcrPdfDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getOcrDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessOcrPdfDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    public String getOcrAltoDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getOcrDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessOcrAltoDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    public String getOcrXmlDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getOcrDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessOcrXmlDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    public String getImportDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getProcessDataDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessImportDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    public String getExportDirectory() throws SwapException, DAOException, IOException, InterruptedException {
        return getProcessDataDirectory() + VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getProcessExportDirectoryName(), this)
        + FileSystems.getDefault().getSeparator();
    }

    public String getProcessDataDirectoryIgnoreSwapping() throws IOException, InterruptedException, SwapException, DAOException {
        String pfad = this.help.getGoobiDataDirectory() + this.id.intValue() + FileSystems.getDefault().getSeparator();
        if (!ConfigurationHelper.getInstance().isAllowWhitespacesInFolder()) {
            pfad = pfad.replaceAll(" ", "__");
        }
        FilesystemHelper.createDirectory(pfad);
        return pfad;
    }

    /**
     * Get the process thumbnail directory which is located directly in the process directory and named 'thumbs'
     * 
     * @return The full path to the thumbnail directory ending with a path separator
     * @throws IOException
     * @throws InterruptedException
     * @throws SwapException
     * @throws DAOException
     */
    public String getThumbsDirectory() throws IOException, InterruptedException, SwapException, DAOException {
        String pfad = getProcessDataDirectory() + "thumbs" + FileSystems.getDefault().getSeparator();
        return pfad;
    }

    /**
     * Return all thumbnail directories containing thumbs for the images stored in the given images directory as a map hashed by the thumbnail size.
     * If no thumbnail directories exist, anfalse empty map is returned
     * 
     * @param imageDirectory The path of an image directory, either only the name or the entire path.
     * @return A map of folders containing thumbnails for the given imageDirectory hashed by thumbnail size
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     */
    public Map<Integer, String> getThumbsDirectories(String imageDirectory) throws IOException, InterruptedException, SwapException, DAOException {
        final String thumbsDirectory = getThumbsDirectory();
        final String imageDirectoryFinal = Paths.get(imageDirectory).getFileName().toString(); //only use the directory name, not the entire path
        return StorageProvider.getInstance()
                .listDirNames(thumbsDirectory)
                .stream()
                .filter(dir -> dir.matches(imageDirectoryFinal + "_\\d{1,9}"))
                .collect(Collectors.toMap(dir -> Integer.parseInt(dir.substring(dir.lastIndexOf("_") + 1)), dir -> thumbsDirectory + dir));
    }

    /**
     * Return the thumbnail directory for the given imageDirectory containing images of the given size. If no directory exists for the given size then
     * the thumbnail directory with the closest larger image size is returned. If no such directory exists, null is returned
     * 
     * @param imageDirectory The path of an image directory, either only the name or the entire path.
     * @param size The size of the desired thumbnails
     * @return The full path to thumbnail directory or null if no matching directory was found
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     */
    public String getThumbsDirectory(String imageDirectory, Integer size) throws IOException, InterruptedException, SwapException, DAOException {
        Map<Integer, String> dirMap = getThumbsDirectories(imageDirectory);
        Optional<Integer> bestSize = dirMap.keySet().stream().filter(dirSize -> dirSize >= size).sorted().findFirst();
        if (bestSize.isPresent()) {
            return dirMap.get(bestSize.get());
        } else {
            return null;
        }
    }

    /**
     * Return the thumbnail directory for the given imageDirectory containing images of the largest size. If no such directory exists, null is
     * returned
     * 
     * @param imageDirectory The path of an image directory, either only the name or the entire path.
     * @return The full path to the largest thumbnail directory or null if no matching directory was found
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     */
    public String getLargestThumbsDirectory(String imageDirectory) throws IOException, InterruptedException, SwapException, DAOException {
        Map<Integer, String> dirMap = getThumbsDirectories(imageDirectory);
        Optional<Integer> bestSize = dirMap.keySet().stream().sorted((i1, i2) -> i2.compareTo(i1)).findFirst();
        if (bestSize.isPresent()) {
            return dirMap.get(bestSize.get());
        } else {
            return null;
        }
    }

    /**
     * Return the thumbnail directory for the given imageDirectory containing images of the given size. If no directory exists for the given size then
     * the thumbnail directory with the closest larger image size is returned. If no such directory exists, the thumbnail directory for the given
     * image directory with the largest thumbnails is returned
     * 
     * @param imageDirectory The path of an image directory, either only the name or the entire path.
     * @param size The size of the desired thumbnails
     * @return The full path to thumbnail directory or null if no matching directory was found
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     */
    public String getThumbsDirectoryOrSmaller(String imageDirectory, Integer size)
            throws IOException, InterruptedException, SwapException, DAOException {
        String bestDir = getThumbsDirectory(imageDirectory, size);
        if (bestDir != null) {
            return bestDir;
        } else {
            return getThumbsDirectories(imageDirectory).entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getKey(), e1.getKey()))
                    .findFirst()
                    .map(entry -> entry.getValue())
                    .orElse(null);
        }
    }

    /**
     * Return the thumbnail directory for the given imageDirectory containing images of the given size. If no directory exists for the given size then
     * the thumbnail directory with the closest larger image size is returned. If no such directory exists, null is returned
     * 
     * @param imageDirectory The path of an image directory, either only the name or the entire path.
     * @param size The size of the desired thumbnails
     * @return The full path to thumbnail directory or the full path to the given imageDirectory if no matching thumbnail directory was found
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     */
    public String getThumbsOrImageDirectory(String imageDirectory, Integer size)
            throws IOException, InterruptedException, SwapException, DAOException {
        String bestDir = getThumbsDirectory(imageDirectory, size);
        if (bestDir != null) {
            return bestDir;
        } else if (imageDirectory.startsWith(getImagesDirectory())) {
            return imageDirectory;
        } else {
            return getImagesDirectory() + imageDirectory;
        }
    }

    /**
     * Get the image sizes of all thumbnail folders for the given image Folder
     * 
     * @param imageDirectory
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws SwapException
     * @throws DAOException
     */
    public List<Integer> getThumbsSizes(String imageDirectory) throws IOException, InterruptedException, SwapException, DAOException {
        return new ArrayList<>(getThumbsDirectories(imageDirectory).keySet());
    }

    /*
     * Helper
     */

    public Project getProjekt() {
        if (projekt == null && projectId != null) {
            try {
                projekt = ProjectManager.getProjectById(projectId);
            } catch (DAOException e) {
                log.error(e);
            }
        }
        return this.projekt;
    }

    public int getSchritteSize() {

        return getSchritte().size();

    }

    public List<Step> getSchritteList() {

        return getSchritte();
    }

    //    public int getHistorySize() {
    //
    //        if (this.history == null) {
    //            return 0;
    //        } else {
    //            return this.history.size();
    //        }
    //    }
    //
    //    public List<HistoryEvent> getHistoryList() {
    //
    //        List<HistoryEvent> temp = new ArrayList<HistoryEvent>();
    //        if (this.history != null) {
    //            temp.addAll(this.history);
    //        }
    //        return temp;
    //    }

    public int getEigenschaftenSize() {
        return getEigenschaften().size();
    }

    public List<Processproperty> getEigenschaftenList() {
        return getEigenschaften();
    }

    public int getWerkstueckeSize() {

        return getWerkstuecke().size();
    }

    public List<Masterpiece> getWerkstueckeList() {

        return getWerkstuecke();
    }

    public int getVorlagenSize() {

        //        if (this.getVorlagen == null) {
        //            this.vorlagen = new ArrayList<Vorlage>();
        //        }
        return this.getVorlagen().size();
    }

    public List<Template> getVorlagenList() {

        return getVorlagen();
    }

    public Integer getSortHelperArticles() {
        if (this.sortHelperArticles == null) {
            this.sortHelperArticles = 0;
        }
        return this.sortHelperArticles;
    }

    public Integer getSortHelperImages() {
        if (this.sortHelperImages == null) {
            this.sortHelperImages = 0;
        }
        return this.sortHelperImages;
    }

    public Integer getSortHelperMetadata() {
        if (this.sortHelperMetadata == null) {
            this.sortHelperMetadata = 0;
        }
        return this.sortHelperMetadata;
    }

    public Integer getSortHelperDocstructs() {
        if (this.sortHelperDocstructs == null) {
            this.sortHelperDocstructs = 0;
        }
        return this.sortHelperDocstructs;
    }

    public boolean isInAuswahllisteAnzeigen() {
        return this.inAuswahllisteAnzeigen;
    }

    public void setInAuswahllisteAnzeigen(boolean inAuswahllisteAnzeigen) {
        this.inAuswahllisteAnzeigen = inAuswahllisteAnzeigen;
    }

    public boolean isPanelAusgeklappt() {
        return this.panelAusgeklappt;
    }

    public void setPanelAusgeklappt(boolean panelAusgeklappt) {
        this.panelAusgeklappt = panelAusgeklappt;
    }

    public Step getAktuellerSchritt() {
        for (Step step : getSchritteList()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.OPEN || step.getBearbeitungsstatusEnum() == StepStatus.INWORK
                    || step.getBearbeitungsstatusEnum() == StepStatus.INFLIGHT) {
                return step;
            }
        }
        return null;
    }

    public boolean isSelected() {
        return (this.selected == null ? false : this.selected);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getErstellungsdatumAsString() {
        return Helper.getDateAsFormattedString(this.erstellungsdatum);
    }

    /*
     * Auswertung des Fortschritts
     */

    public String getFortschritt() {
        int offen = 0;
        int inBearbeitung = 0;
        int abgeschlossen = 0;
        for (Step step : getSchritte()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE || step.getBearbeitungsstatusEnum() == StepStatus.DEACTIVATED) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.LOCKED) {
                offen++;
            } else {
                inBearbeitung++;
            }
        }
        double offen2 = 0;
        double inBearbeitung2 = 0;
        double abgeschlossen2 = 0;

        if ((offen + inBearbeitung + abgeschlossen) == 0) {
            offen = 1;
        }
        offen2 = (offen * 100) / (double) (offen + inBearbeitung + abgeschlossen);
        inBearbeitung2 = (inBearbeitung * 100) / (double) (offen + inBearbeitung + abgeschlossen);
        abgeschlossen2 = 100 - offen2 - inBearbeitung2;
        // (abgeschlossen * 100) / (offen + inBearbeitung + abgeschlossen);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#000");
        return df.format(abgeschlossen2) + df.format(inBearbeitung2) + df.format(offen2);

    }

    public double getFortschritt1() {
        double offen = 0;
        double inBearbeitung = 0;
        double error = 0;
        double abgeschlossen = 0;
        for (Step step : getSchritte()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.LOCKED) {
                offen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.ERROR) {
                error++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.DEACTIVATED) {
                // nothing
            } else {
                inBearbeitung++;
            }
        }
        if ((offen + inBearbeitung + error + abgeschlossen) == 0) {
            offen = 1;
        }
        return (offen * 100) / (offen + inBearbeitung + error + abgeschlossen);
    }

    public double getFortschritt2() {
        double offen = 0;
        double inBearbeitung = 0;
        double error = 0;
        double abgeschlossen = 0;
        for (Step step : getSchritte()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.LOCKED) {
                offen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.ERROR) {
                error++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.DEACTIVATED) {
                // nothing
            } else {
                inBearbeitung++;
            }
        }
        if ((offen + inBearbeitung + error + abgeschlossen) == 0) {
            offen = 1;
        }
        return (inBearbeitung * 100) / (offen + inBearbeitung + error + abgeschlossen);
    }

    public double getFortschrittError() {
        double offen = 0;
        double inBearbeitung = 0;
        double error = 0;
        double abgeschlossen = 0;
        for (Step step : getSchritte()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.LOCKED) {
                offen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.ERROR) {
                error++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.DEACTIVATED) {
                // nothing
            } else {
                inBearbeitung++;
            }
        }
        if ((offen + inBearbeitung + error + abgeschlossen) == 0) {
            offen = 1;
        }
        return (error * 100) / (offen + inBearbeitung + error + abgeschlossen);
    }

    public double getFortschritt3() {
        double offen = 0;
        double inBearbeitung = 0;
        double error = 0;
        double abgeschlossen = 0;

        for (Step step : getSchritte()) {
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.LOCKED) {
                offen++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.ERROR) {
                error++;
            } else if (step.getBearbeitungsstatusEnum() == StepStatus.DEACTIVATED) {
                // nothing
            } else {
                inBearbeitung++;
            }
        }
        if ((offen + inBearbeitung + error + abgeschlossen) == 0) {
            offen = 1;
        }
        double offen2 = 0;
        double inBearbeitung2 = 0;
        double error2 = 0;
        double abgeschlossen2 = 0;

        offen2 = (offen * 100) / (offen + inBearbeitung + error + abgeschlossen);
        error2 = (error * 100) / (offen + inBearbeitung + error + abgeschlossen);
        inBearbeitung2 = (inBearbeitung * 100) / (offen + inBearbeitung + error + abgeschlossen);
        abgeschlossen2 = 100 - offen2 - inBearbeitung2 - error2;
        return abgeschlossen2;
    }

    public String getMetadataFilePath() throws IOException, InterruptedException, SwapException, DAOException {
        return getProcessDataDirectory() + "meta.xml";
    }

    public String getTemplateFilePath() throws IOException, InterruptedException, SwapException, DAOException {
        return getProcessDataDirectory() + "template.xml";
    }

    public String getFulltextFilePath() throws IOException, InterruptedException, SwapException, DAOException {
        return getProcessDataDirectory() + "fulltext.xml";
    }

    public Fileformat readMetadataFile()
            throws ReadException, IOException, InterruptedException, PreferencesException, SwapException, DAOException, WriteException {
        if (!checkForMetadataFile()) {
            throw new IOException(Helper.getTranslation("metadataFileNotFound") + " " + getMetadataFilePath());
        }
        // TODO check for younger temp files

        /* prüfen, welches Format die Metadaten haben (Mets, xstream oder rdf */
        String type = MetadatenHelper.getMetaFileType(getMetadataFilePath());
        Fileformat ff = MetadatenHelper.getFileformatByName(type, regelsatz);
        if (ff == null) {
            String[] parameter = { titel, type };
            Helper.setFehlerMeldung(Helper.getTranslation("MetadataFormatNotAvailable", parameter));
            return null;
        }
        try {
            ff.read(getMetadataFilePath());
        } catch (ReadException e) {
            if (e.getMessage().startsWith("Parse error at line -1")) {
                Helper.setFehlerMeldung("metadataCorrupt");
            } else {
                throw e;
            }
        }
        return ff;
    }

    private boolean checkForMetadataFile()
            throws IOException, InterruptedException, SwapException, DAOException, WriteException, PreferencesException {
        boolean result = true;
        Path f = Paths.get(getMetadataFilePath());
        if (!StorageProvider.getInstance().isFileExists(f)) {
            result = false;
        }

        return result;
    }

    public synchronized void writeMetadataFile(Fileformat gdzfile)
            throws IOException, InterruptedException, SwapException, DAOException, WriteException, PreferencesException {

        String path = this.getProcessDataDirectory();
        int maximumNumberOfBackups = ConfigurationHelper.getInstance().getNumberOfMetaBackups();

        // Backup meta.xml
        String metaFileName = "meta.xml";
        Path metaFile = Paths.get(path + metaFileName);
        String backupMetaFileName = Process.createBackup(path, metaFileName, maximumNumberOfBackups);
        Path backupMetaFile = Paths.get(path + backupMetaFileName);

        // Backup meta_anchor.xml
        String metaAnchorFileName = "meta_anchor.xml";
        Path metaAnchorFile = Paths.get(path + metaAnchorFileName);
        String backupMetaAnchorFileName = Process.createBackup(path, metaAnchorFileName, maximumNumberOfBackups);
        Path backupMetaAnchorFile = Paths.get(path + backupMetaAnchorFileName);

        Fileformat ff = MetadatenHelper.getFileformatByName(getProjekt().getFileFormatInternal(), this.regelsatz);

        synchronized (xmlWriteLock) {
            ff.setDigitalDocument(gdzfile.getDigitalDocument());
            try {
                ff.write(path + metaFileName);
            } catch (UGHException ughException) {
                // Error while writing meta.xml or meta_anchor.xml. Restore backups and rethrow error

                // Restore meta.xml
                if ((!Files.exists(metaFile) || Files.size(metaFile) == 0) && Files.exists(backupMetaFile)) {
                    Files.copy(backupMetaFile, metaFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Restore meta_anchor.xml
                if ((!Files.exists(metaAnchorFile) || Files.size(metaAnchorFile) == 0) && Files.exists(backupMetaAnchorFile)) {
                    Files.copy(backupMetaAnchorFile, metaAnchorFile, StandardCopyOption.REPLACE_EXISTING);
                }

                throw ughException;
            }
        }
        Map<String, List<String>> metadata = MetadatenHelper.getMetadataOfFileformat(gdzfile, false);

        MetadataManager.updateMetadata(id, metadata);

        Map<String, List<String>> jsonMetadata = MetadatenHelper.getMetadataOfFileformat(gdzfile, true);

        MetadataManager.updateJSONMetadata(id, jsonMetadata);
    }

    private static String createBackup(String path, String fileName, int maximumNumberOfBackups) {
        String backupFileName;
        try {
            backupFileName = BackupFileManager.createBackup(path, path, fileName, maximumNumberOfBackups, true);
            // Output in the GUI is already done in BackupFileManager.createBackup()
        } catch (IOException ioException) {
            backupFileName = null;
            // Output in the GUI is already done in BackupFileManager.createBackup()
        }
        return backupFileName;
    }

    public void saveTemporaryMetsFile(Fileformat gdzfile)
            throws SwapException, DAOException, IOException, InterruptedException, PreferencesException, WriteException {

        int maximumNumberOfBackups = ConfigurationHelper.getInstance().getNumberOfMetaBackups();
        Process.createBackup(this.getProcessDataDirectory(), "temp.xml", maximumNumberOfBackups);

        Fileformat ff = MetadatenHelper.getFileformatByName(getProjekt().getFileFormatInternal(), this.regelsatz);
        ff.setDigitalDocument(gdzfile.getDigitalDocument());
        ff.write(getProcessDataDirectory() + "temp.xml");
    }

    public void writeMetadataAsTemplateFile(Fileformat inFile)
            throws IOException, InterruptedException, SwapException, DAOException, WriteException, PreferencesException {
        inFile.write(getTemplateFilePath());
    }

    public Fileformat readMetadataAsTemplateFile()
            throws ReadException, IOException, InterruptedException, PreferencesException, SwapException, DAOException {
        if (StorageProvider.getInstance().isFileExists(Paths.get(getTemplateFilePath()))) {
            Fileformat ff = null;
            String type = MetadatenHelper.getMetaFileType(getTemplateFilePath());
            if (log.isDebugEnabled()) {
                log.debug("current template.xml file type: " + type);
            }
            ff = MetadatenHelper.getFileformatByName(type, regelsatz);
            ff.read(getTemplateFilePath());
            return ff;
        } else {
            throw new IOException("File does not exist: " + getTemplateFilePath());
        }
    }

    public void removeTemporaryMetadataFiles() {
        DirectoryStream.Filter<Path> filter = new FileListFilter("temp\\.?(_anchor)?.xml.*+");

        try {
            List<Path> temporaryFiles = StorageProvider.getInstance().listFiles(getProcessDataDirectory(), filter);
            if (!temporaryFiles.isEmpty()) {
                for (Path file : temporaryFiles) {
                    StorageProvider.getInstance().deleteDir(file);
                }
            }
        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }
    }

    public boolean getTemporaryMetadataFiles() {
        return checkForNewerTemporaryMetadataFiles();
    }

    public void setTemporaryMetadataFiles(boolean value) {
    }

    public void overwriteMetadata() {
        try {
            String path = this.getProcessDataDirectory();
            Path temporaryFile = Paths.get(path, "temp.xml");
            Path temporaryAnchorFile = Paths.get(path, "temp_anchor.xml");

            int maximumNumberOfBackups = ConfigurationHelper.getInstance().getNumberOfMetaBackups();

            if (StorageProvider.getInstance().isFileExists(temporaryFile)) {
                // backup meta.xml
                Process.createBackup(path, "meta.xml", maximumNumberOfBackups);

                // copy temp.xml to meta.xml
                Path meta = Paths.get(path, "meta.xml");
                StorageProvider.getInstance().copyFile(temporaryFile, meta);
            }
            if (StorageProvider.getInstance().isFileExists(temporaryAnchorFile)) {
                // backup meta_anchor.xml
                Process.createBackup(path, "meta_anchor.xml", maximumNumberOfBackups);

                // copy temp_anchor.xml to meta_anchor.xml
                Path metaAnchor = Paths.get(path, "meta_anchor.xml");
                StorageProvider.getInstance().copyFile(temporaryAnchorFile, metaAnchor);
            }

        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }
    }

    public boolean checkForNewerTemporaryMetadataFiles() {
        try {
            Path temporaryFile = Paths.get(getProcessDataDirectory(), "temp.xml");
            if (StorageProvider.getInstance().isFileExists(temporaryFile)) {
                Path metadataFile = Paths.get(getMetadataFilePath());
                long tempTime = StorageProvider.getInstance().getLastModifiedDate(temporaryFile);
                //              (FileTime) Files.getAttribute(temporaryFile, "unix:lastModifiedTime");
                long metaTime = StorageProvider.getInstance().getLastModifiedDate(metadataFile);
                //                        (FileTime) Files.getAttribute(metadataFile, "unix:lastModifiedTime");
                //                return tempTime.toMillis() > metaTime.toMillis();
                return tempTime > metaTime;

            }
        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }

        return false;
    }

    /**
     * prüfen, ob der Vorgang Schritte enthält, die keinem Benutzer und keiner Benutzergruppe zugewiesen ist
     * ================================================================
     */
    public boolean getContainsUnreachableSteps() {
        if (getSchritteList().size() == 0) {
            return true;
        }
        for (Step s : getSchritteList()) {
            if (s.getBenutzergruppenSize() == 0 && s.getBenutzerSize() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if there is one task in edit mode, where the user has the rights to write to image folder
     * ================================================================
     */
    public boolean isImageFolderInUse() {
        for (Step s : getSchritteList()) {
            if (s.getBearbeitungsstatusEnum() == StepStatus.INWORK && s.isTypImagesSchreiben()) {
                return true;
            }
        }
        return false;
    }

    /**
     * get user of task in edit mode with rights to write to image folder ================================================================
     */
    public User getImageFolderInUseUser() {
        for (Step s : getSchritteList()) {
            if (s.getBearbeitungsstatusEnum() == StepStatus.INWORK && s.isTypImagesSchreiben()) {
                return s.getBearbeitungsbenutzer();
            }
        }
        return null;
    }

    /**
     * here differet Getters and Setters for the same value, because Hibernate does not like bit-Fields with null Values (thats why Boolean) and
     * MyFaces seams not to like Boolean (thats why boolean for the GUI) ================================================================
     */
    public Boolean isSwappedOutHibernate() {
        return this.swappedOut;
    }

    public void setSwappedOutHibernate(Boolean inSwappedOut) {
        this.swappedOut = inSwappedOut;
    }

    public boolean isSwappedOutGui() {
        if (this.swappedOut == null) {
            this.swappedOut = false;
        }
        return this.swappedOut;
    }

    public void setSwappedOutGui(boolean inSwappedOut) {
        this.swappedOut = inSwappedOut;
    }

    public void downloadXML() {
        XsltPreparatorDocket xmlExport = new XsltPreparatorDocket();
        try {
            String ziel = Helper.getCurrentUser().getHomeDir() + getTitel() + "_log.xml";
            xmlExport.startExport(this, ziel);
        } catch (IOException e) {
            Helper.setFehlerMeldung("could not write logfile to home directory: ", e);
        } catch (InterruptedException e) {
            Helper.setFehlerMeldung("could not execute command to write logfile to home directory", e);
        }
    }

    /**
     * download xml logfile for current process
     */
    public void downloadLogFile() {

        XsltPreparatorDocket xmlExport = new XsltPreparatorDocket();
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = getTitel() + "_log.xml";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            // write to servlet output stream
            try {
                ServletOutputStream out = response.getOutputStream();
                xmlExport.startExport(this, out);
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
        return;
    }

    public String downloadDocket() {

        if (log.isDebugEnabled()) {
            log.debug("generate docket for process " + this.id);
        }
        String rootpath = ConfigurationHelper.getInstance().getXsltFolder();
        Path xsltfile = Paths.get(rootpath, "docket.xsl");
        if (docket != null) {
            xsltfile = Paths.get(rootpath, docket.getFile());
            if (!StorageProvider.getInstance().isFileExists(xsltfile)) {
                Helper.setFehlerMeldung("docketMissing");
                return "";
            }
        }
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = this.titel + ".pdf";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            // write run note to servlet output stream
            try {
                ServletOutputStream out = response.getOutputStream();
                XsltToPdf ern = new XsltToPdf();
                ern.startExport(this, out, xsltfile.toString(), new XsltPreparatorDocket());
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
        return "";
    }

    /**
     * generate simplified set of structure and metadata to provide a PDF generation for printing
     */
    public void downloadSimplifiedMetadataAsPDF() {
        log.debug("generate simplified metadata xml for process " + this.id);
        String rootpath = ConfigurationHelper.getInstance().getXsltFolder();
        Path xsltfile = Paths.get(rootpath, "docket_metadata.xsl");

        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = this.titel + ".pdf";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            // write simplified metadata to servlet output stream
            try {
                //            	XsltPreparatorSimplifiedMetadata xslt = new XsltPreparatorSimplifiedMetadata();
                //                try {
                //                	LoginBean login = (LoginBean) Helper.getManagedBeanValue("#{LoginForm}");
                //                    String ziel = login.getMyBenutzer().getHomeDir() + this.getTitel() + "_log.xml";
                //                    xslt.startExport(this, ziel);
                //                } catch (Exception e) {
                //                    Helper.setFehlerMeldung("Could not write logfile to home directory", e);
                //                }

                ServletOutputStream out = response.getOutputStream();
                XsltToPdf ern = new XsltToPdf();
                ern.startExport(this, out, xsltfile.toString(), new XsltPreparatorMetadata());
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting simplefied metadata", e);
            }

            facesContext.responseComplete();
        }
    }

    public Step getFirstOpenStep() {

        for (Step s : getSchritteList()) {
            if (s.getBearbeitungsstatusEnum().equals(StepStatus.OPEN) || s.getBearbeitungsstatusEnum().equals(StepStatus.INWORK)
                    || s.getBearbeitungsstatusEnum() == StepStatus.INFLIGHT) {
                return s;
            }
        }
        return null;
    }

    public String getMethodFromName(String methodName) {
        java.lang.reflect.Method method;
        try {
            method = this.getClass().getMethod(methodName);
            Object o = method.invoke(this);
            return (String) o;
        } catch (SecurityException e) {

        } catch (NoSuchMethodException e) {

        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

        try {
            String imagefolder = this.getImagesDirectory();

            String foldername = VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getAdditionalProcessFolderName(methodName), this);
            if (StringUtils.isNotBlank(foldername)) {
                String folder = imagefolder + foldername;
                if (StorageProvider.getInstance().isFileExists(Paths.get(folder))) {
                    return folder;
                }
            }
        } catch (SwapException e) {

        } catch (DAOException e) {

        } catch (IOException e) {

        } catch (InterruptedException e) {

        }

        return null;
    }

    public Docket getDocket() {
        if (docket == null && docketId != null) {
            try {
                docket = DocketManager.getDocketById(docketId);
            } catch (DAOException e) {
                log.error(e);
            }
        }
        return docket;
    }

    @Override
    public int compareTo(Process arg0) {

        return id.compareTo(arg0.getId());
    }

    @Override
    public void lazyLoad() {
    }

    @Override
    public Process clone() {
        Process p = new Process();
        p.setDocket(docket);
        p.setInAuswahllisteAnzeigen(false);
        p.setIstTemplate(istTemplate);
        p.setProjectId(projectId);
        p.setProjekt(projekt);
        p.setRegelsatz(regelsatz);
        p.setSortHelperStatus(sortHelperStatus);
        p.setTitel(this.getTitel() + "_copy");

        this.bhelp.SchritteKopieren(this, p);
        this.bhelp.ScanvorlagenKopieren(this, p);
        this.bhelp.WerkstueckeKopieren(this, p);
        this.bhelp.EigenschaftenKopieren(this, p);
        LoginBean loginForm = Helper.getLoginBean();

        for (Step step : p.getSchritteList()) {

            step.setBearbeitungszeitpunkt(p.getErstellungsdatum());
            step.setEditTypeEnum(StepEditType.AUTOMATIC);
            if (loginForm != null) {
                step.setBearbeitungsbenutzer(loginForm.getMyBenutzer());
            }

            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {
                step.setBearbeitungsbeginn(p.getErstellungsdatum());
                // this concerns steps, which are set as done right on creation
                // bearbeitungsbeginn is set to creation timestamp of process
                // because the creation of it is basically begin of work
                Date myDate = new Date();
                step.setBearbeitungszeitpunkt(myDate);
                step.setBearbeitungsende(myDate);
            }

        }

        try {

            ProcessManager.saveProcess(p);
        } catch (DAOException e) {
            log.error("error on save: ", e);
        }

        return p;
    }

    public void addLogEntry() {
        if (uploadedFile != null) {
            saveUploadedFile();
        } else {

            LogEntry entry = new LogEntry();
            entry.setCreationDate(new Date());
            entry.setType(LogType.USER);
            entry.setProcessId(id);
            LoginBean loginForm = Helper.getLoginBean();
            entry.setUserName(loginForm.getMyBenutzer().getNachVorname());
            entry.setContent(content);
            content = "";

            entry.setSecondContent(secondContent);
            secondContent = "";

            entry.setThirdContent(thirdContent);
            thirdContent = "";
            processLog.add(entry);

            ProcessManager.saveLogEntry(entry);
        }
    }

    public List<StringPair> getMetadataList() {
        if (metadataList.isEmpty()) {
            metadataList = MetadataManager.getMetadata(id);
        }
        return metadataList;
    }

    /**
     * getter for the representative as IIIF URL of the configured thumbnail size
     * 
     * @return IIIF URL for the representative thumbnail image
     */
    public String getRepresentativeImage() {
        int thumbnailWidth = ConfigurationHelper.getInstance().getMetsEditorThumbnailSize();
        return getRepresentativeImage(thumbnailWidth);
    }

    /**
     * convert the path of the representative into a IIIF URL of the given size
     * 
     * @param thumbnailWidth max width of the image
     * @return IIIF URL for the representative image
     */
    public String getRepresentativeImage(int thumbnailWidth) {
        try {
            String thumbnail = getRepresentativeImageAsString();
            Path imagePath = Paths.get(thumbnail);
            if (StorageProvider.getInstance().isFileExists(imagePath)) {
                //            Image image = new Image(Paths.get(representativeImage), 0, thumbnailWidth);
                Image image = new Image(this, imagePath.getParent().getFileName().toString(), imagePath.getFileName().toString(), 0, thumbnailWidth);
                return image.getThumbnailUrl();
            } else {
                FacesContext context = FacesContextHelper.getCurrentFacesContext();
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                String scheme = request.getScheme(); // http
                String serverName = request.getServerName(); // hostname.com
                int serverPort = request.getServerPort(); // 80
                String contextPath = request.getContextPath(); // /mywebapp
                StringBuilder sb = new StringBuilder();
                sb.append(scheme);
                sb.append("://");
                sb.append(serverName);
                sb.append(":");
                sb.append(serverPort);
                sb.append(contextPath);
                sb.append("/");
                sb.append(thumbnail);
                sb.append("&amp;width=");
                sb.append(thumbnailWidth);
                sb.append("&amp;height=");
                sb.append(thumbnailWidth);
                return sb.toString();
            }
        } catch (IOException | InterruptedException | SwapException | DAOException e) {
            log.error("Error creating representative image url for process " + this.getId());
            String rootpath = "cs?action=image&format=jpg&sourcepath=file:///";
            return rootpath + representativeImage.replaceAll("\\\\", "/");
        }
    }

    /**
     * get the path of the representative as string from the filesystem
     * 
     * @return path of representative image
     */
    public String getRepresentativeImageAsString() {
        if (StringUtils.isBlank(representativeImage)) {
            int imageNo = 0;
            if (!getMetadataList().isEmpty()) {
                for (StringPair sp : getMetadataList()) {
                    if (sp.getOne().equals("_representative")) {
                        imageNo = NumberUtils.toInt(sp.getTwo()) - 1;
                    }
                }
            }
            try {
                List<Path> images = StorageProvider.getInstance().listFiles(getImagesTifDirectory(true), NIOFileUtils.imageNameFilter);
                if (images == null || images.size() == 0) {
                    images = StorageProvider.getInstance().listFiles(getImagesOrigDirectory(true), NIOFileUtils.imageNameFilter);
                }
                if (images != null && !images.isEmpty()) {
                    representativeImage = images.get(imageNo).toString();
                } else {
                    images = StorageProvider.getInstance().listFiles(getImagesTifDirectory(true), NIOFileUtils.objectNameFilter);
                    if (images == null || images.size() == 0) {
                        images = StorageProvider.getInstance().listFiles(getImagesOrigDirectory(true), NIOFileUtils.objectNameFilter);
                    }
                    if (images != null && !images.isEmpty()) {
                        return "uii/template/img/goobi_3d_object_placeholder.png?version=1";
                    }
                }

                if (StringUtils.isBlank(representativeImage)) {
                    return "uii/template/img/thumbnail-placeholder.png?version=1";
                }
            } catch (IOException | InterruptedException | SwapException | DAOException e) {
                log.error(e);
            }
        }
        return representativeImage;
    }

    public Map<Path, List<Path>> getAllFolderAndFiles() {
        Map<Path, List<Path>> folderAndFileMap = new HashMap<>();
        try {
            List<Path> imageFolders = StorageProvider.getInstance().listFiles(getImagesDirectory(), NIOFileUtils.folderFilter);
            List<Path> thumbFolders = StorageProvider.getInstance().listFiles(getThumbsDirectory(), NIOFileUtils.folderFilter);
            List<Path> ocrFolders = StorageProvider.getInstance().listFiles(getOcrDirectory(), NIOFileUtils.folderFilter);

            for (Path folder : imageFolders) {
                folderAndFileMap.put(folder, StorageProvider.getInstance().listFiles(folder.toString(), NIOFileUtils.fileFilter));
                // add subfolders to the list as well (e.g. for LayoutWizzard)
                for (Path subfolder : StorageProvider.getInstance().listFiles(folder.toString(), NIOFileUtils.folderFilter)) {
                    folderAndFileMap.put(subfolder, StorageProvider.getInstance().listFiles(subfolder.toString(), NIOFileUtils.fileFilter));
                }
            }
            for (Path folder : thumbFolders) {
                folderAndFileMap.put(folder, StorageProvider.getInstance().listFiles(folder.toString(), NIOFileUtils.fileFilter));
            }
            for (Path folder : ocrFolders) {
                folderAndFileMap.put(folder, StorageProvider.getInstance().listFiles(folder.toString(), NIOFileUtils.fileFilter));
            }
        } catch (IOException | InterruptedException | SwapException | DAOException e) {

            log.error(e);
        }

        return folderAndFileMap;
    }

    // this method is needed for ajaxPlusMinusButton.xhtml
    public String getTitelLokalisiert() {
        return titel;
    }

    /**
     * return specific variable for this process adapted by the central VariableReplacer
     * 
     * @param inVariable
     * @return adapted result with replaced value
     */
    public String getReplacedVariable(String inVariable) {
        // if replaced value is not stored already then do it now
        if (!tempVariableMap.containsKey(inVariable)) {
            DigitalDocument dd = null;
            Prefs myPrefs = null;
            // just load the mets file if needed
            if (inVariable.startsWith("{meta")) {
                myPrefs = getRegelsatz().getPreferences();
                try {
                    Fileformat gdzfile = readMetadataFile();
                    dd = gdzfile.getDigitalDocument();
                } catch (Exception e) {
                    log.error("error reading METS file for process " + id, e);
                }
            }
            VariableReplacer replacer = new VariableReplacer(dd, myPrefs, this, null);
            // put replaced value into temporary store
            String replacedValue = replacer.replace(inVariable);
            //  return empty string, if value could not be found
            if (replacedValue.equals(inVariable)) {
                replacedValue = "";
            }
            tempVariableMap.put(inVariable, replacedValue);
        }

        return tempVariableMap.get(inVariable);
    }

    /**
     * Get all Step titles for steps of a given status as String with a given separator
     * 
     * @param status long value for the status (0=Locked, 1=Open, 2=InWork, 3=Done, 4=Error, 5=Deactivated)
     * @param separator String value to use as separator
     * @return status as String with a given separator
     */
    public String getStepsAsString(long status, String separator) {
        StringBuilder result = new StringBuilder();

        if (schritte != null && !schritte.isEmpty()) {
            for (Step s : schritte) {
                if (s.getBearbeitungsstatusEnum().getValue() == status) {
                    if (result.length() > 0) {
                        result.append(separator);
                    }
                    result.append(s.getTitelLokalisiert());
                }
            }
        }
        return result.toString();
    }

    /**
     * Get a list of folder names to select from. Not all folder can be selected.
     * 
     * @return
     */

    public List<SelectItem> getVisibleFolder() {
        if (folderList.isEmpty()) {
            try {
                folderList.add(new SelectItem(getExportDirectory(), Helper.getTranslation("process_log_file_exportFolder")));
                folderList.add(new SelectItem(getImportDirectory(), Helper.getTranslation("process_log_file_importFolder")));
                //                folderList.add(new SelectItem(getSourceDirectory(), Helper.getTranslation("process_log_file_sourceFolder")));
                folderList.add(new SelectItem(getImagesTifDirectory(false), Helper.getTranslation("process_log_file_mediaFolder")));
                if (ConfigurationHelper.getInstance().isUseMasterDirectory()) {
                    folderList.add(new SelectItem(getImagesOrigDirectory(false), Helper.getTranslation("process_log_file_masterFolder")));
                }

                Iterator<String> configuredImageFolder = ConfigurationHelper.getInstance().getLocalKeys("process.folder.images");
                while (configuredImageFolder.hasNext()) {
                    String keyName = configuredImageFolder.next();
                    String folderName = keyName.replace("process.folder.images.", "");
                    if (!"master".equals(folderName) && !"main".equals(folderName)) {
                        String folder = getConfiguredImageFolder(folderName);
                        if (StringUtils.isNotBlank(folder) && StorageProvider.getInstance().isFileExists(Paths.get(folder))) {
                            folderList.add(new SelectItem(folder, Helper.getTranslation(folderName)));
                        }

                        //                        folderList.add(new SelectItem(getImagesTifDirectory(false), Helper.getTranslation("process_log_file_mediaFolder")));
                        //                    } else {
                        //                        folderList.add(new SelectItem(getConfiguredImageFolder(folderName), Helper.getTranslation(folderName)));
                    }
                }

            } catch (SwapException | DAOException | IOException | InterruptedException e) {
                log.error(e);
            }
        }
        return folderList;
    }

    /**
     * List the files of a selected folder. If a LogEntry is used (because it was uploaded in the logfile area), it will be used. Otherwise a
     * temporary LogEntry is created.
     * 
     * @return
     */

    public List<LogEntry> getFilesInSelectedFolder() {
        if (StringUtils.isBlank(currentFolder)) {
            return Collections.emptyList();
        }
        // enable/disable option to delete a file
        if (currentFolder.endsWith("/import/") || currentFolder.endsWith("/export/") || currentFolder.endsWith("_source")) {
            showFileDeletionButton = true;
        } else {
            showFileDeletionButton = false;
        }

        List<Path> files = StorageProvider.getInstance().listFiles(currentFolder);
        List<LogEntry> answer = new ArrayList<>();
        // check if LogEntry exist
        for (Path file : files) {
            boolean matchFound = false;
            for (LogEntry entry : processLog) {
                if (entry.getType() == LogType.FILE && StringUtils.isNotBlank(entry.getThirdContent())
                        && entry.getThirdContent().equals(file.toString())) {
                    entry.setFile(file);
                    answer.add(entry);
                    matchFound = true;
                    break;
                }
            }
            // otherwise create one
            if (!matchFound) {
                LogEntry entry = new LogEntry();
                entry.setContent(""); // comment
                entry.setSecondContent(currentFolder); // folder
                entry.setThirdContent(file.toString()); // absolute path
                entry.setFile(file);
                answer.add(entry);
            }
        }

        return answer;
    }

    /**
     * Download a selected file
     * 
     * @param entry
     */

    public void downloadFile(LogEntry entry) {
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        Path path = entry.getFile();
        if (path == null) {
            path = Paths.get(entry.getThirdContent());
        }
        String fileName = path.getFileName().toString();
        String contentType = facesContext.getExternalContext().getMimeType(fileName);
        try {
            int contentLength = (int) StorageProvider.getInstance().getFileSize(path);
            response.reset();
            response.setContentType(contentType);
            response.setContentLength(contentLength);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            OutputStream output = response.getOutputStream();
            try (InputStream inp = StorageProvider.getInstance().newInputStream(path)) {
                IOUtils.copy(inp, output);
            }
            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Delete a LogEntry and the file belonging to it
     * 
     * @param entry
     */

    public void deleteFile(LogEntry entry) {
        Path path = entry.getFile();
        if (path == null) {
            path = Paths.get(entry.getThirdContent());
        }
        // check if log entry has an id
        if (entry.getId() != null) {
            // if yes, delete entry
            String filename = entry.getBasename();

            processLog.remove(entry);
            ProcessManager.deleteLogEntry(entry);

            // create a new entry to document the deletion
            LogEntry deletionInfo = LogEntry.build(id)
                    .withContent(Helper.getTranslation("processlogFileDeleted", filename))
                    .withCreationDate(new Date())
                    .withType(LogType.INFO)
                    .withUsername(Helper.getCurrentUser().getNachVorname());
            processLog.add(deletionInfo);
            ProcessManager.saveLogEntry(deletionInfo);
        }
        // delete file
        try {
            StorageProvider.getInstance().deleteFile(path);
        } catch (IOException e) {
            log.error(e);
        }

    }

    /**
     * Save the previous uploaded file in the selected process directory and create a new LogEntry.
     * 
     */

    public void saveUploadedFile() {

        Path folder = null;
        try {
            if (uploadFolder.equals("intern")) {
                folder = Paths.get(getProcessDataDirectory(), ConfigurationHelper.getInstance().getFolderForInternalProcesslogFiles());
            } else {
                folder = Paths.get(getExportDirectory());
            }
            if (!StorageProvider.getInstance().isFileExists(folder)) {
                StorageProvider.getInstance().createDirectories(folder);
            }
            Path destination = Paths.get(folder.toString(), basename);
            StorageProvider.getInstance().move(tempFileToImport, destination);
            LogEntry entry = LogEntry.build(id)
                    .withCreationDate(new Date())
                    .withContent(content)
                    .withType(LogType.FILE)
                    .withUsername(Helper.getCurrentUser().getNachVorname());
            entry.setSecondContent(folder.toString());
            entry.setThirdContent(destination.toString());
            ProcessManager.saveLogEntry(entry);
            processLog.add(entry);

        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }
        uploadedFile = null;
        content = "";
    }

    /**
     * Upload a file and save it as a temporary file
     * 
     */
    public void uploadFile() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (this.uploadedFile == null) {
                Helper.setFehlerMeldung("noFileSelected");
                return;
            }

            basename = getFileName(this.uploadedFile);
            if (basename.startsWith(".")) {
                basename = basename.substring(1);
            }
            if (basename.contains("/")) {
                basename = basename.substring(basename.lastIndexOf("/") + 1);
            }
            if (basename.contains("\\")) {
                basename = basename.substring(basename.lastIndexOf("\\") + 1);
            }
            tempFileToImport = Files.createTempFile(basename, "");
            inputStream = this.uploadedFile.getInputStream();
            outputStream = new FileOutputStream(tempFileToImport.toString());

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Helper.setFehlerMeldung("uploadFailed");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
    }

    /**
     * extract the filename for the uploaded file
     * 
     * @param part
     * @return
     */

    private String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    /**
     * Change the process title and rename folders, property values ezc
     */

    public boolean changeProcessTitle(String newTitle) {

        /* Prozesseigenschaften */
        if (getEigenschaftenList() != null && !getEigenschaftenList().isEmpty()) {
            for (Processproperty pe : this.getEigenschaftenList()) {
                if (pe != null && pe.getWert() != null) {
                    if (pe.getWert().contains(this.getTitel())) {
                        pe.setWert(pe.getWert().replaceAll(this.getTitel(), newTitle));
                    }
                }
            }
        }
        /* Scanvorlageneigenschaften */
        if (getVorlagenList() != null && !getVorlagenList().isEmpty()) {
            for (Template vl : this.getVorlagenList()) {
                for (Templateproperty ve : vl.getEigenschaftenList()) {
                    if (ve.getWert().contains(this.getTitel())) {
                        ve.setWert(ve.getWert().replaceAll(this.getTitel(), newTitle));
                    }
                }
            }
        }
        /* Werkstückeigenschaften */
        if (getWerkstueckeList() != null && !getWerkstueckeList().isEmpty()) {
            for (Masterpiece w : this.getWerkstueckeList()) {
                for (Masterpieceproperty we : w.getEigenschaftenList()) {
                    if (we.getWert().contains(this.getTitel())) {
                        we.setWert(we.getWert().replaceAll(this.getTitel(), newTitle));
                    }
                }
            }
        }
        try {
            {
                // renaming image directories
                String imageDirectory = getImagesDirectory();
                Path dir = Paths.get(imageDirectory);
                if (StorageProvider.getInstance().isFileExists(dir) && StorageProvider.getInstance().isDirectory(dir)) {
                    List<Path> subdirs = StorageProvider.getInstance().listFiles(imageDirectory);
                    for (Path imagedir : subdirs) {
                        if (StorageProvider.getInstance().isDirectory(imagedir) || StorageProvider.getInstance().isSymbolicLink(imagedir)) {
                            StorageProvider.getInstance().move(imagedir, Paths.get(imagedir.toString().replace(getTitel(), newTitle)));
                        }
                    }
                }
            }
            {
                // renaming ocr directories
                String ocrDirectory = getOcrDirectory();
                Path dir = Paths.get(ocrDirectory);
                if (StorageProvider.getInstance().isFileExists(dir) && StorageProvider.getInstance().isDirectory(dir)) {
                    List<Path> subdirs = StorageProvider.getInstance().listFiles(ocrDirectory);
                    for (Path imagedir : subdirs) {
                        if (StorageProvider.getInstance().isDirectory(imagedir) || StorageProvider.getInstance().isSymbolicLink(imagedir)) {
                            StorageProvider.getInstance().move(imagedir, Paths.get(imagedir.toString().replace(getTitel(), newTitle)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.trace("could not rename folder", e);
        }

        if (!this.isIstTemplate()) {
            /* Tiffwriter-Datei löschen */
            GoobiScript gs = new GoobiScript();
            List<Integer> pro = new ArrayList<>();
            pro.add(this.getId());
            gs.deleteTiffHeaderFile(pro);

            // update paths in metadata file
            try {
                Fileformat fileFormat = readMetadataFile();

                UghHelper ughhelp = new UghHelper();
                MetadataType mdt = ughhelp.getMetadataType(this, "pathimagefiles");
                DocStruct physical = fileFormat.getDigitalDocument().getPhysicalDocStruct();
                List<? extends ugh.dl.Metadata> alleImagepfade = physical.getAllMetadataByType(mdt);
                if (alleImagepfade.size() > 0) {
                    for (Metadata md : alleImagepfade) {
                        fileFormat.getDigitalDocument().getPhysicalDocStruct().getAllMetadata().remove(md);
                    }
                }
                Metadata newmd = new Metadata(mdt);
                String newFolder = getImagesTifDirectory(false).replace(getTitel(), newTitle);
                if (SystemUtils.IS_OS_WINDOWS) {
                    newmd.setValue("file:/" + newFolder);
                } else {
                    newmd.setValue("file://" + newFolder);
                }
                fileFormat.getDigitalDocument().getPhysicalDocStruct().addMetadata(newmd);

                if (physical.getAllChildren() != null) {
                    for (DocStruct page : physical.getAllChildren()) {
                        List<ContentFile> contentFileList = page.getAllContentFiles();
                        if (contentFileList != null) {
                            for (ContentFile cf : contentFileList) {
                                cf.setLocation(cf.getLocation().replace(getTitel(), newTitle));
                            }
                        }
                    }
                }

                writeMetadataFile(fileFormat);

            } catch (IOException | InterruptedException | SwapException | DAOException | UghHelperException | UGHException e) {
                log.info("Could not rename paths in metadata file", e);
            }
        }
        /* Vorgangstitel */
        this.setTitel(newTitle);

        return true;
    }

    /**
     * get the complete path to a folder as string, or null if the folder name is unknown
     * 
     * @param folderName
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws SwapException
     * @throws DAOException
     */

    public String getConfiguredImageFolder(String folderName) throws IOException, InterruptedException, SwapException, DAOException {
        if ("master".equals(folderName)) {
            return getImagesOrigDirectory(false);
        } else if ("main".equals(folderName) || "media".equals(folderName)) {
            return getImagesTifDirectory(false);
        }

        String imagefolder = this.getImagesDirectory();
        String foldername = VariableReplacer.simpleReplace(ConfigurationHelper.getInstance().getAdditionalProcessFolderName(folderName), this);
        if (StringUtils.isNotBlank(foldername)) {
            String folder = imagefolder + foldername;
            return folder;
        }
        return null;
    }

    /**
     * Get the date of the last finished step
     */

    public String getLastStatusChangeDate() {
        Date date = null;
        if (ConfigurationHelper.getInstance().isProcesslistShowEditionData()) {
            if (schritte != null) {
                for (Step step : schritte) {
                    if (step.getBearbeitungsstatusEnum() == StepStatus.DONE && step.getBearbeitungsende() != null) {
                        if (date == null) {
                            date = step.getBearbeitungsende();
                        } else if (date.before(step.getBearbeitungsende())) {
                            date = step.getBearbeitungsende();
                        }
                    }
                }
            }
        }
        return date == null ? "" : Helper.getDateAsFormattedString(date);
    }

    /**
     * Get the user name of the last finished step
     */

    public String getLastStatusChangeUser() {
        String username = "";
        if (ConfigurationHelper.getInstance().isProcesslistShowEditionData()) {
            Step task = null;
            if (schritte != null) {
                for (Step step : schritte) {
                    if (step.getBearbeitungsstatusEnum() == StepStatus.DONE && step.getBearbeitungsende() != null) {
                        if (task == null) {
                            task = step;
                        } else if (task.getBearbeitungsende().before(step.getBearbeitungsende())) {
                            task = step;
                        }
                    }
                }
            }
            if (task != null) {
                if (task.getEditTypeEnum() == StepEditType.AUTOMATIC || task.getEditTypeEnum() == StepEditType.ADMIN) {
                    username = Helper.getTranslation(task.getEditTypeEnum().getTitle());
                } else if (task.getBearbeitungsbenutzer() != null) {
                    username = task.getBearbeitungsbenutzer().getNachVorname();
                }
            }
        }
        return username;
    }

    /**
     * Get the name of the last finished task
     * 
     */

    public String getLastStatusChangeTask() {
        String taskname = "";
        if (ConfigurationHelper.getInstance().isProcesslistShowEditionData()) {
            Step task = null;
            if (schritte != null) {
                for (Step step : schritte) {
                    if (step.getBearbeitungsstatusEnum() == StepStatus.DONE && step.getBearbeitungsende() != null) {
                        if (task == null) {
                            task = step;
                        } else if (task.getBearbeitungsende().before(step.getBearbeitungsende())) {
                            task = step;
                        }
                    }
                }
            }
            if (task != null) {
                taskname = task.getNormalizedTitle();
            }
        }
        return taskname;
    }

    public void setPauseAutomaticExecution(boolean pauseAutomaticExecution) {
        List<Step> automaticTasks = new ArrayList<>();

        if (this.pauseAutomaticExecution && !pauseAutomaticExecution) {
            // search any open tasks; check if they are automatic tasks; start them
            for (Step step : schritte) {
                if (step.isTypAutomatisch()) {
                    switch (step.getBearbeitungsstatusEnum()) {
                        case DEACTIVATED:
                        case DONE:
                        case ERROR:
                        case LOCKED:
                            break;
                        case INFLIGHT:
                        case INWORK:
                        case OPEN:
                            automaticTasks.add(step);
                    }
                }
            }
        }
        this.pauseAutomaticExecution = pauseAutomaticExecution;
        if (!automaticTasks.isEmpty()) {
            for (Step step : automaticTasks) {
                //We need to set the process in the step to this process, so the step doesn't fetch the process from
                //the DB when it checks if automatic execution is paused
                step.setProzess(this);
                ScriptThreadWithoutHibernate script = new ScriptThreadWithoutHibernate(step);
                script.startOrPutToQueue();
            }
        }
    }

    //read the image comments files in the image folders, and return all of them as a list.
    public List<ImageComment> getImageComments() throws IOException, InterruptedException, SwapException, DAOException {

        List<ImageComment> lstComments = new ArrayList<>();

        ImageCommentHelper helper = new ImageCommentHelper();

        String folderMaster = this.getImagesOrigDirectory(true);
        HashMap<String, String> masterComments = helper.getComments(folderMaster);

        for (String imageName : masterComments.keySet()) {
            String comment = masterComments.get(imageName);
            if (!StringUtils.isBlank(comment)) {
                lstComments.add(new ImageComment("Master", imageName, comment));
            }
        }

        if (StorageProvider.getInstance().isFileExists(Paths.get(this.getImagesDirectory()))) {
            String folderMedia = this.getImagesTifDirectory(true);
            HashMap<String, String> mediaComments = helper.getComments(folderMedia);

            for (String imageName : mediaComments.keySet()) {
                String comment = mediaComments.get(imageName);
                if (!StringUtils.isBlank(comment)) {
                    lstComments.add(new ImageComment("Media", imageName, comment));
                }
            }
        }

        return lstComments;
    }

    public List<String> getArchivedImageFolders() throws IOException, InterruptedException, SwapException, DAOException {
        if (this.id == null) {
            return new ArrayList<>();
        }
        List<String> filesInImages = StorageProvider.getInstance().list(this.getImagesDirectory());
        return filesInImages.stream()
                .filter(p -> p.endsWith(".xml"))
                .map(p -> Paths.get(p).getFileName().toString().replace(".xml", ""))
                .collect(Collectors.toList());
    }
}