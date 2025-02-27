package io.goobi.workflow.xslt;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.tuple.MutablePair;
import org.goobi.beans.Process;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.metadaten.Image;
import de.sub.goobi.metadaten.MetadatenHelper;
import lombok.extern.log4j.Log4j2;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Person;
import ugh.dl.Prefs;

/**
 * This class provides a simplified export of all metadata into a xml file
 * 
 * @author Steffen Hankiewicz
 */
@Log4j2
public class XsltPreparatorMetadata implements IXsltPreparator {
    private static Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");

    private MetadatenHelper metahelper;
    private Prefs prefs;

    /**
     * This method exports the METS metadata as xml to a given directory
     * 
     * @param p the process to export
     * @param destination the destination to write the file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ExportFileException
     */

    public void startExport(Process p, String destination) throws FileNotFoundException, IOException {
        startExport(p, new FileOutputStream(destination), null);
    }

    public void startExport(Process p, Path dest) throws FileNotFoundException, IOException {
        startExport(p, new FileOutputStream(dest.toFile()), null);
    }

    /**
     * This method exports the METS metadata as xml to a given stream.
     * 
     * @param process the process to export
     * @param os the OutputStream to write the contents to
     * @throws IOException
     * @throws ExportFileException
     */
    @Override
    public void startExport(Process process, OutputStream os, String xslt) throws IOException {
        try {
            this.prefs = process.getRegelsatz().getPreferences();
            Fileformat ff = process.readMetadataFile();
            DigitalDocument document = ff.getDigitalDocument();
            this.metahelper = new MetadatenHelper(prefs, document);

            Document doc = createDocument(process, true);

            XMLOutputter outp = new XMLOutputter();
            outp.setFormat(Format.getPrettyFormat());
            outp.output(doc, os);
            os.close();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * This method creates a new xml document with process metadata
     * 
     * @param process the process to export
     * @return a new xml document
     * @throws ConfigurationException
     */
    public Document createDocument(Process process, boolean addNamespace) {

        Element mainElement = new Element("process");
        Document doc = new Document(mainElement);

        mainElement.setAttribute("processID", String.valueOf(process.getId()));

        Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");
        mainElement.setNamespace(xmlns);
        // namespace declaration
        if (addNamespace) {
            Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            mainElement.addNamespaceDeclaration(xsi);
            Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd", xsi);
            mainElement.setAttribute(attSchema);
        }

        // process information
        Element processTitle = new Element("title", xmlns);
        processTitle.setText(process.getTitel());
        mainElement.addContent(processTitle);

        Element project = new Element("project", xmlns);
        project.setText(process.getProjekt().getTitel());
        mainElement.addContent(project);

        Element date = new Element("creationDate", xmlns);
        date.setText(String.valueOf(process.getErstellungsdatum()));
        mainElement.addContent(date);

        Element pdfdate = new Element("pdfGenerationDate", xmlns);
        pdfdate.setText(String.valueOf(new Date()));
        mainElement.addContent(pdfdate);

        Element ruleset = new Element("ruleset", xmlns);
        ruleset.setText(process.getRegelsatz().getDatei());
        mainElement.addContent(ruleset);

        try {
            Element representative = new Element("representative", xmlns);
            Path repImagePath = Paths.get(process.getRepresentativeImageAsString());
            Image repimage = new Image(repImagePath, 0, 30000);
            representative.setAttribute("path", process.getRepresentativeImageAsString());
            representative.setAttribute("url", repimage.getThumbnailUrl());
            mainElement.addContent(representative);
        } catch (IOException e1) {
            log.error("Error added the representative to the metadata docket", e1);
        }

        // add all important mets content
        try {
            Fileformat ff = process.readMetadataFile();
            if (ff != null) {
                DigitalDocument dd = ff.getDigitalDocument();
                DocStruct logicalTopstruct = dd.getLogicalDocStruct();

                addMetadataAndChildElements(logicalTopstruct, mainElement);
            }
        } catch (Exception e) {
            log.error("Error while creating a pdf file", e);
        }
        return doc;
    }

    /**
     * add a node for each docstruct and add all metadata
     * 
     * @param parentStruct the parent structure element to analyze
     * @param parentNode the parent node where to add the subnodes to
     */
    private void addMetadataAndChildElements(DocStruct parentStruct, Element parentNode) {
        Element node = new Element("node", xmlns);
        node.setAttribute("type", parentStruct.getType().getNameByLanguage(Helper.getMetadataLanguage()));

        addPersonElements(parentStruct, node);
        if (parentStruct.getAllMetadata() != null) {
            for (Metadata md : parentStruct.getAllMetadata()) {
                if (md.getValue() != null && md.getValue().length() > 0) {
                    Element metadata = new Element("metadata", xmlns);
                    metadata.setAttribute("name", md.getType().getNameByLanguage(Helper.getMetadataLanguage()));
                    metadata.addContent(md.getValue());
                    node.addContent(metadata);
                }
            }
        }

        //pages 
        MutablePair<String, String> first = this.metahelper.getImageNumber(parentStruct, MetadatenHelper.PAGENUMBER_FIRST);
        MutablePair<String, String> last = this.metahelper.getImageNumber(parentStruct, MetadatenHelper.PAGENUMBER_LAST);

        if (first != null) {
            Element mdPages = new Element("metadata", xmlns);
            mdPages.setAttribute("name", Helper.getTranslation("Pages"));
            mdPages.addContent(first.getRight() + " - " + last.getRight());
            node.addContent(mdPages);
            
            Element mdImages = new Element("metadata", xmlns);
            mdImages.setAttribute("name", Helper.getTranslation("Images"));
            mdImages.addContent(first.getLeft() + " - " + last.getLeft());
            node.addContent(mdImages);
        }

        if (parentStruct.getAllChildren() != null) {
            for (DocStruct ds : parentStruct.getAllChildren()) {
                addMetadataAndChildElements(ds, node);
            }
        }

        parentNode.addContent(node);
    }

    /**
     * add all persons
     * 
     * @param parentStruct the parent structure element to analyze
     * @param parentNode the parent node where to add the subnodes to
     */
    private void addPersonElements(DocStruct parentStruct, Element parentNode) {
        if (parentStruct.getAllPersons() != null) {
            for (Person p : parentStruct.getAllPersons()) {
                Element pele = new Element("person", xmlns);
                pele.setAttribute("role", p.getType().getNameByLanguage(Helper.getMetadataLanguage()));
                if (p.getFirstname() != null) {
                    pele.setAttribute("firstname", p.getFirstname());
                }
                if (p.getLastname() != null) {
                    pele.setAttribute("lastname", p.getLastname());
                }
                if (p.getAuthorityURI() != null) {
                    pele.setAttribute("uri", p.getAuthorityURI());
                }
                if (p.getAuthorityValue() != null) {
                    pele.setAttribute("id", p.getAuthorityValue());
                }
                parentNode.addContent(pele);
            }
        }
    }

    /**
     * This method exports the production metadata for a list of processes as a single file to a given stream.
     * 
     * @param processList
     * @param outputStream
     * @param xslt
     */
    public void startExport(List<Process> processList, OutputStream outputStream, String xslt) {
        Document answer = new Document();
        Element root = new Element("processes");
        answer.setRootElement(root);
        Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");

        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addNamespaceDeclaration(xsi);
        root.setNamespace(xmlns);
        Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd", xsi);
        root.setAttribute(attSchema);
        for (Process p : processList) {
            Document doc = createDocument(p, false);
            Element processRoot = doc.getRootElement();
            processRoot.detach();
            root.addContent(processRoot);
        }

        XMLOutputter outp = new XMLOutputter();
        outp.setFormat(Format.getPrettyFormat());
        try {
            outp.output(answer, outputStream);
        } catch (IOException e) {
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    outputStream = null;
                }
            }
        }
    }
}
