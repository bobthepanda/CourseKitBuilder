package csb.file;

import static csb.CSB_StartupConstants.PATH_SITES;
import csb.data.Assignment;
import csb.data.Course;
import csb.data.CoursePage;
import csb.data.Instructor;
import csb.data.Lecture;
import csb.data.ScheduleItem;
import csb.gui.WebBrowser;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import javafx.geometry.Insets;
import javax.swing.text.html.HTML;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This class is responsible for exporting schedule.html to its proper
 * directory. Note that it uses a base file in the baseDir directory, which gets
 * loaded first and that each course will have its own file exported to a
 * directory in the sitesDir directory.
 *
 * @author Richard McKenna
 */
public class CourseSiteExporter {

    // THERE ARE A NUMBER OF CONSTANTS THAT WE'LL USE FOR FINDING
    // ELEMENTS IN THE PAGES WE'RE LOADING, AS WELL AS THINGS WE'LL
    // BUILD INTO OUR PAGE WHILE EXPORTING
    public static final String ID_NAVBAR = "navbar";
    public static final String ID_BANNER = "banner";
    public static final String ID_SCHEDULE = "schedule";
    public static final String ID_HOME_LINK = "home_link";
    public static final String ID_SYLLABUS_LINK = "syllabus_link";
    public static final String ID_SCHEDULE_LINK = "schedule_link";
    public static final String ID_HWS_LINK = "hws_link";
    public static final String ID_PROJECTS_LINK = "projects_link";
    public static final String ID_INSTRUCTOR_LINK = "instructor_link";
    public static final String ID_INLINED_COURSE = "inlined_course";
    public static final String CLASS_NAV = "nav";
    public static final String CLASS_OPEN_NAV = "open_nav";
    public static final String CLASS_SCH = "sch";
    public static final String CLASS_HOLIDAY = "holiday";
    public static final String CLASS_LECTURE = "lecture";
    public static final String CLASS_HW = "hw";
    public static final String CLASS_HWS = "hws";

    // THIS IS TEXT WE'LL BE ADDING TO OUR PAGE
    public static final String INDEX_HEADER = "Home";
    public static final String SYLLABUS_HEADER = "Syllabus";
    public static final String SCHEDULE_HEADER = "Schedule";
    public static final String HWS_HEADER = "HWs";
    public static final String PROJECTS_HEADER = "Projects";
    public static final String MONDAY_HEADER = "MONDAY";
    public static final String TUESDAY_HEADER = "TUESDAY";
    public static final String WEDNESDAY_HEADER = "WEDNESDAY";
    public static final String THURSDAY_HEADER = "THURSDAY";
    public static final String FRIDAY_HEADER = "FRIDAY";
    public static final String LECTURE_HEADER = "Lecture ";
    public static final String DUE_HEADER = "due @ 11:59pm";

    // THESE ARE THE POSSIBLE SITE PAGES OUR SCHEDULE PAGE
    // MAY NEED TO LINK TO
    public static String INDEX_PAGE = "index.html";
    public static String SYLLABUS_PAGE = "syllabus.html";
    public static String SCHEDULE_PAGE = "schedule.html";
    public static String HWS_PAGE = "hws.html";
    public static String PROJECTS_PAGE = "projects.html";

    // THIS IS THE DIRECTORY STRUCTURE USED BY OUR SITE
    public static final String CSS_DIR = "css";
    public static final String IMAGES_DIR = "images";

    // AND SOME TEXT WE'LL NEED TO ADD ON THE FLY
    public static final String SLASH = "/";
    public static final String DASH = " - ";
    public static final String LINE_BREAK = "<br />";

    // THESE ARE THE DIRECTORIES WHERE OUR BASE SCHEDULE
    // FILE IS AND WHERE OUR COURSE SITES WILL BE EXPORTED TO
    String baseDir;
    String sitesDir;

    // WE'LL USE THIS VARIABLE TO KEEP TRACK OF EXPORTING PROGRESS
    double perc = 0;
    int pageIndex;

    /**
     * This constructor initializes this exporter to load the schedule page from
     * the initBaseDir and export course pages to directories found in
     * initSitesDir.
     *
     * @param initBaseDir Directory that contains the base site files.
     *
     * @param initSitesDir Directory where course sites will be exported to.
     * Note that each course will have a directory here containing its site.
     */
    public CourseSiteExporter(String initBaseDir, String initSitesDir) {
        baseDir = initBaseDir;
        sitesDir = initSitesDir;
    }

    /**
     * This method is the facade to a lot of work done to export the site. It
     * will setup the necessary course directory if it doesn't already exist and
     * copy the needed stylesheets and images and will then export the necessary
     * pages.
     *
     * @param courseToExport Course whose site is being built.
     *
     * @throws IOException This exception is thrown when a problem occurs
     * creating the course site directory and/or files.
     */
    public void exportCourseSite(Course courseToExport) throws Exception {
        // GET THE DIRECTORY TO EXPORT THE SITE
        String courseExportPath = (new File(sitesDir) + SLASH)
                + courseToExport.getSubject() + courseToExport.getNumber();

        // FIRST EXPORT ANCILLARY FILES LIKE STYLE SHEETS AND IMAGES. NOTE
        // THAT THIS ONLY NEEDS TO BE DONE ONCE FOR EACH COURSE
        if (!new File(courseExportPath).exists()) {
            setupCourseSite(courseExportPath);
        }

        CoursePage[] pages = CoursePage.values();
        List<CoursePage> listedPages = courseToExport.getPages();

        //SETTING PROGRESS BAR WINDOW
        Stage progressStage = new Stage();
        ReentrantLock progressLock = new ReentrantLock();
        VBox box = new VBox();
        box.setSpacing(20);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.setPrefWidth(500);
        box.setAlignment(Pos.CENTER);

        Label processLabel = new Label("Exporting");
        processLabel.setFont(Font.font("Franklin Gothic", 36));
        HBox toolbar = new HBox();
        toolbar.setSpacing(20);
        toolbar.setPadding(new Insets(20, 20, 20, 20));
        ProgressBar bar = new ProgressBar(0);
        ProgressIndicator indicator = new ProgressIndicator(0);
        toolbar.getChildren().add(bar);
        toolbar.getChildren().add(indicator);
        toolbar.setAlignment(Pos.CENTER);

        box.getChildren().add(processLabel);
        box.getChildren().add(toolbar);

        Scene scene = new Scene(box);
        progressStage.setScene(scene);
        progressStage.show();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    int donePages = 0;
                    progressLock.lock();
                    for (pageIndex = 0; pageIndex < pages.length; pageIndex++) {
                        donePages += 1;
                        perc = (1.00 * donePages) / listedPages.size();
                        CoursePage page = pages[pageIndex];
                        if (courseToExport.hasCoursePage(page)) {
                            // CALCULATE THE PROGRESS
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    processLabel.setText("Exporting " + page.toString() + " page");
                                    bar.setProgress(perc);
                                    indicator.setProgress(perc);
                                }
                            });
                            exportPage(pages[pageIndex], courseToExport, courseExportPath);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                } finally {
                    //HIDE PROGRESS WINDOW WHEN COMPLETED & SHOW NEW PAGES
                    progressLock.unlock();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Stage webBrowserStage = new Stage();
                            try {
                                WebBrowser webBrowser = new WebBrowser(webBrowserStage, getPageURLPath(courseToExport, CoursePage.SCHEDULE));
                            }
                            catch (Exception e) {}
                            webBrowserStage.show();
                            progressStage.hide();
                        }
                    });
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * This function exports the course pages to html files.
     *
     * @param page Page to export.
     * @param courseToExport Course whose site we are to export.
     * @param courseExportPath The directory where courseToExport's site pages
     * are to be exported to.
     *
     * @throws IOException Thrown when there is a problem exporting the schedule
     * page for this site.
     */
    public void exportPage(CoursePage page, Course courseToExport, String courseExportPath)
            throws IOException {
        try {
            // NOW THAT EVERYTHING IS SETUP, BUILD THE PAGE DOCUMENT
            Document doc;
            String pageFile;

            if (page == CoursePage.INDEX) {
                doc = buildIndexPage(courseToExport);
                pageFile = INDEX_PAGE;
            } else if (page == CoursePage.SYLLABUS) {
                doc = buildSyllabusPage(courseToExport);
                pageFile = SYLLABUS_PAGE;
            } else if (page == CoursePage.SCHEDULE) {
                doc = buildSchedulePage(courseToExport);
                pageFile = SCHEDULE_PAGE;
            } else if (page == CoursePage.HWS) {
                doc = buildHWsPage(courseToExport);
                pageFile = HWS_PAGE;
            } else {
                doc = buildProjectsPage(courseToExport);
                pageFile = PROJECTS_PAGE;
            }

            // AND SAVE IT TO A FILE
            saveDocument(doc, courseExportPath + SLASH + pageFile);

            // NOTE THAT IF ANYTHING GOES WRONG WE WILL REFLECT AND/OR PASS ALL EXCEPTIONS
        } catch (TransformerException | SAXException | ParserConfigurationException exception) {
            // WE ARE GOING TO REFLECT ALL OF THESE EXCEPTIONS AS
            // IOExceptions, WHICH WE'LL HANDLE TOGETHER
            //throw new IOException(exception.getMessage());
            System.out.println("COULD NOT LOAD PAGE");
        }
    }

    /**
     * Builds and returns the path to access the type of page denoted by cP for
     * the given course argument.
     *
     * @param course The course for which we want to access a link.
     * @param cP The particular page in the course site for accessing a link.
     *
     * @return A textual path to the page we wish to link to.
     */
    public String getPageURLPath(Course course, CoursePage cP) {
        String urlPath = PATH_SITES + course.getSubject()
                + course.getNumber()
                + SLASH + this.getLink(cP);
        File webPageFile = new File(urlPath);
        try {
            URL pageURL = webPageFile.toURI().toURL();
            return pageURL.toString();
        } catch (MalformedURLException murle) {
            return null;
        }
    }

    // BELOW ARE ALL THE PRIVATE HELPER METHODS
    private Document initDoc(Course courseToExport, CoursePage page, String pageFileName) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // BUILD THE PATH
        String path = baseDir + SLASH + pageFileName;

        // NOW LOAD THE DOCUMENT
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(path);

        // UPDATE THE PAGE HEADER
        Node titleNode = doc.getElementsByTagName(HTML.Tag.TITLE.toString()).item(0);
        titleNode.setTextContent(courseToExport.getSubject() + " "
                + courseToExport.getNumber());

        // ADD THE NAVBAR LINKS
        appendNavbarLinks(doc, courseToExport, page);

        // SET THE BANNER
        setBanner(doc, courseToExport);

        // AND ADD THE INSTRUCTOR
        appendInstructor(doc, courseToExport.getInstructor());

        // AND RETURN
        return doc;
    }

    // BUILDS AN INDEX PAGE AND RETURNS IT AS A SINGLE Document
    private Document buildIndexPage(Course courseToExport) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // GET A NEW DOC
        Document indexDoc = initDoc(courseToExport, CoursePage.INDEX, INDEX_PAGE);

        // NOW DO THE STUFF SPECIFIC TO AN INDEX PAGE
        fillInlinedCourseDescription(indexDoc, courseToExport);

        // AND RETURN THE FULL PAGE DOM
        return indexDoc;
    }

    // BUILDS AN INDEX PAGE AND RETURNS IT AS A SINGLE Document
    private Document buildSyllabusPage(Course courseToExport) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // GET A NEW DOC
        Document doc = initDoc(courseToExport, CoursePage.SYLLABUS, SYLLABUS_PAGE);

        // NOW DO THE STUFF SPECIFIC TO AN INDEX PAGE
        // @todo
        // AND RETURN THE FULL PAGE DOM
        return doc;
    }

    // BUILDS A SCHEDULE PAGE AND RETURNS IT AS A SINGLE Document
    private Document buildSchedulePage(Course courseToExport) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // GET A NEW DOC
        Document scheduleDoc = initDoc(courseToExport, CoursePage.SCHEDULE, SCHEDULE_PAGE);

        // NOW BUILD THE SCHEDULE TABLE
        fillScheduleTable(scheduleDoc, courseToExport);

        // AND RETURN THE FULL PAGE DOM
        return scheduleDoc;
    }

    // BUILDS A HWS PAGE AND RETURNS IT AS A SINGLE Document
    private Document buildHWsPage(Course courseToExport) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // GET A NEW DOC
        Document hwsDoc = initDoc(courseToExport, CoursePage.HWS, HWS_PAGE);

        // MISSING UPDATING THE TABLE
        fillHwsTable(hwsDoc, courseToExport);

        // AND RETURN THE FULL PAGE DOM
        return hwsDoc;
    }

    // BUILDS A HWS PAGE AND RETURNS IT AS A SINGLE Document
    private Document buildProjectsPage(Course courseToExport) throws SAXException, TransformerException, IOException, ParserConfigurationException {
        // GET A NEW DOC
        Document projectsDoc = initDoc(courseToExport, CoursePage.PROJECTS, PROJECTS_PAGE);

        // PROJECTS PAGES DON'T HAVE ANYTHING INTERESTING WHEN THE SEMESTER STARTS
        // AND RETURN THE FULL PAGE DOM
        return projectsDoc;
    }

    // INITIALIZES ALL THE HELPER FILES AND DIRECTORIES, LIKE FOR CSS
    private void setupCourseSite(String exportPath) throws IOException {
        // FIRST MAKE THE FOLDERS
        File siteDir = new File(exportPath);
        siteDir.mkdir();
        File cssDir = new File(exportPath + SLASH + CSS_DIR);
        cssDir.mkdir();
        File imagesDir = new File(exportPath + SLASH + IMAGES_DIR);
        imagesDir.mkdir();

        // THEN COPY THE STYLESHEETS OVER
        File baseCSSDir = new File(baseDir + "/" + CSS_DIR);
        File[] cssFiles = baseCSSDir.listFiles();
        for (int i = 0; i < cssFiles.length; i++) {
            File cssFile = new File(cssDir + SLASH + cssFiles[i].getName());
            Files.copy(cssFiles[i].toPath(), cssFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // AND THEN COPY THE IMAGES OVER
        File baseImagesDir = new File(baseDir + "/" + IMAGES_DIR);
        File[] imageFiles = baseImagesDir.listFiles();
        for (int i = 0; i < imageFiles.length; i++) {
            File imageFile = new File(imagesDir + "/" + imageFiles[i].getName());
            Files.copy(imageFiles[i].toPath(), imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // APPENDS THE ISNTRUCTOR TO THE BOTTOM OF THE PAGE
    private void appendInstructor(Document pageDoc, Instructor courseInstructor) {
        Node instructorSpan = (Element) getNodeWithId(pageDoc, HTML.Tag.SPAN.toString(), ID_INSTRUCTOR_LINK);
        Element instructorLinkElement = pageDoc.createElement(HTML.Tag.A.toString());
        instructorLinkElement.setAttribute(HTML.Attribute.HREF.toString(), courseInstructor.getHomepageURL());
        instructorLinkElement.setTextContent(courseInstructor.getName());
        instructorSpan.appendChild(instructorLinkElement);
    }

    // FILS THE INLINED COURSE DESCRIPTION IN THE INDEX PAGE
    private void fillInlinedCourseDescription(Document indexDoc, Course courseToExport) {
        Node inlinedCourseSpan = (Element) getNodeWithId(indexDoc, HTML.Tag.SPAN.toString(), ID_INLINED_COURSE);
        Text text = indexDoc.createTextNode(courseToExport.getSubject().toString() + courseToExport.getNumber());
        inlinedCourseSpan.appendChild(text);
    }

    // FILLS IN THE SCHEDULE PAGE'S SCHEDULE TABLE
    private void fillScheduleTable(Document scheduleDoc, Course courseToExport) {
        LocalDate countingDate = courseToExport.getStartingMonday().minusDays(0);
        int lectureCounter = 1;
        ObservableList<Lecture> list = courseToExport.getLectures();
        List<Integer> days = new ArrayList<Integer>();
        for (DayOfWeek day : courseToExport.getLectureDays()) {
            days.add(day.getValue() - 1);
        }
        HashMap<LocalDate, ScheduleItem> scheduleItemMappings = courseToExport.getScheduleItemMappings();
        List<Assignment> hws = courseToExport.getAssignments();
        List<LocalDate> dates = new ArrayList<LocalDate>();
        for (Assignment hw : hws) {
            dates.add(hw.getDate());
        }

        String s = "";
        while (countingDate.isBefore(courseToExport.getEndingFriday())
                || countingDate.isEqual(courseToExport.getEndingFriday())) {
            // ADD THE MONDAY-FRIDAY HEADERS            
            // FIRST FOR EACH WEEK MAKE A TABLE ROW            
            Element dowRowHeaderElement = scheduleDoc.createElement(HTML.Tag.TR.toString());

            // AND ADD DAY OF THE WEEK TABLE HEADERS
            addDayOfWeekHeader(scheduleDoc, dowRowHeaderElement, MONDAY_HEADER);
            addDayOfWeekHeader(scheduleDoc, dowRowHeaderElement, TUESDAY_HEADER);
            addDayOfWeekHeader(scheduleDoc, dowRowHeaderElement, WEDNESDAY_HEADER);
            addDayOfWeekHeader(scheduleDoc, dowRowHeaderElement, THURSDAY_HEADER);
            addDayOfWeekHeader(scheduleDoc, dowRowHeaderElement, FRIDAY_HEADER);

            // NOW ADD ALL THE DAYS    
            Element dowRowElement = scheduleDoc.createElement(HTML.Tag.TR.toString());

            // MONDAY - FRIDAY
            for (int i = 0; i < 5; i++) {
                // FIRST ADD THE DAY, GIVING IT AN ID OF THE DATE ITSELF
                Element dayCell = addDayOfWeekCell(scheduleDoc, dowRowElement, countingDate);

                // IS THERE A SCHEDULE ITEM FOR THAT DAY?
                ScheduleItem scheduleItem = scheduleItemMappings.get(countingDate);
                if (scheduleItem != null) {
                    // SET THE DATE TO A HOLDIAY
                    dayCell.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HOLIDAY);

                    // ADD A LINK
                    Element holidayLinkElement = scheduleDoc.createElement(HTML.Tag.A.toString());
                    holidayLinkElement.setAttribute(HTML.Attribute.HREF.toString(), scheduleItem.getLink());
                    dayCell.appendChild(holidayLinkElement);

                    // ADD THE TEXT TO THE LINK
                    Text linkText = scheduleDoc.createTextNode(scheduleItem.getDescription());
                    dayCell.appendChild(linkText);
                    holidayLinkElement.appendChild(linkText);

                    // AND NOW ADD 6 LINE BREAKS
                    for (int brCounter = 0; brCounter < 8; brCounter++) {
                        Element br = scheduleDoc.createElement(HTML.Tag.BR.toString());
                        dayCell.appendChild(br);
                    }
                } else {
                    // SET THE DATE TO A REGULAR DAY
                    dayCell.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_SCH);

                    // IS IT A LECTURE DAY?
                    if (days.contains(i) && lectureCounter - 1 < list.size()) {

                        // SET LECTURE & NUMBER
                        Element lecture = scheduleDoc.createElement(HTML.Tag.SPAN.toString());
                        lecture.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_LECTURE);
                        lecture.setTextContent("Lecture " + lectureCounter);
                        dayCell.appendChild(lecture);
                        dayCell.appendChild(scheduleDoc.createElement(HTML.Tag.BR.toString()));

                        // APPEND NAME OF TOPIC
                        Lecture l = list.get(lectureCounter - 1);
                        Node topic = scheduleDoc.createTextNode(l.getTopic());
                        dayCell.appendChild(topic);
                        dayCell.appendChild(scheduleDoc.createElement(HTML.Tag.BR.toString()));
                        if (s.length() > 0) {
                            dayCell.appendChild(scheduleDoc.createTextNode(s));
                            dayCell.appendChild(scheduleDoc.createElement(HTML.Tag.BR.toString()));
                        }
                        l.setSessions(l.getSessions() - 1);

                        // CONTROL FOR # OF SESSIONS
                        if (l.getSessions() > 0) {
                            s = "(continued)";
                        } else {
                            lectureCounter += 1;
                            s = "";
                        }
                    }
                }

                // IS THERE HW DUE THAT DAY?
                if (dates.contains(countingDate)) {
                    Assignment today = hws.get(dates.indexOf(countingDate));
                    Element hw = scheduleDoc.createElement(HTML.Tag.SPAN.toString());
                    hw.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HW);
                    hw.setTextContent(today.getName());
                    dayCell.appendChild(hw);
                    dayCell.appendChild(scheduleDoc.createElement(HTML.Tag.BR.toString()));
                    dayCell.appendChild(scheduleDoc.createTextNode("due @11:59PM"));
                    dayCell.appendChild(scheduleDoc.createElement(HTML.Tag.BR.toString()));
                    dayCell.appendChild(scheduleDoc.createTextNode("(" + today.getTopics() + ")"));
                }

                // FIRST SCHEDULE ITEMS
                countingDate = countingDate.plusDays(1);
            }

            // SKIP THE WEEKEND DAYS
            countingDate = countingDate.plusDays(2);

            // AND PUT THEM IN THE TABLE
            Node scheduleTableNode = getNodeWithId(scheduleDoc, HTML.Tag.TABLE.toString(), ID_SCHEDULE);
            scheduleTableNode.appendChild(dowRowHeaderElement);
            scheduleTableNode.appendChild(dowRowElement);

        }
    }

    // ADDS A DAY OF WEEK CELL TO THE SCHEDULE PAGE SCHEDULE TABLE
    private Element addDayOfWeekCell(Document scheduleDoc, Element tableRow, LocalDate date) {
        // MAKE THE TABLE CELL FOR THIS DATE
        Element dateCell = scheduleDoc.createElement(HTML.Tag.TD.toString());
        dateCell.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_SCH);
        dateCell.setAttribute(HTML.Attribute.ID.toString(), date.getMonthValue() + "_" + date.getDayOfMonth());
        tableRow.appendChild(dateCell);

        // THE TEXT FOR THE DATE IS BOLD, SO ADD A STRONG ELEMENT
        Element strong = scheduleDoc.createElement(HTML.Tag.STRONG.toString());
        dateCell.appendChild(strong);

        // AND PUT THE TEXT INSIDE
        Text dateText = scheduleDoc.createTextNode(date.getMonthValue() + "/" + date.getDayOfMonth());
        dateCell.appendChild(dateText);

        // THEN A BR TAG
        Element brElement = scheduleDoc.createElement(HTML.Tag.BR.toString());
        dateCell.appendChild(brElement);

        // AND RETURN THE NEW ELEMENT
        return dateCell;
    }

    // ADDS A DAY OF WEEK HEADER TO THE SCHEDULE PAGE SCHEDULE TABLE
    private void addDayOfWeekHeader(Document scheduleDoc, Element tableRow, String dayOfWeekText) {
        Element dayOfWeekHeader = scheduleDoc.createElement(HTML.Tag.TH.toString());
        dayOfWeekHeader.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_SCH);
        dayOfWeekHeader.setTextContent(dayOfWeekText);
        tableRow.appendChild(dayOfWeekHeader);
    }

    // SETS UP THE TABLE FOR THE HWS PAGE
    private void fillHwsTable(Document scheduleDoc, Course courseToExport) {
        Node table = scheduleDoc.getElementsByTagName(HTML.Tag.TABLE.toString()).item(0);
        List<Assignment> list = courseToExport.getAssignments();
        int r = 240;
        int g = 240;
        int b = 255;
        for (Assignment a : list) {
            Element tableRow = scheduleDoc.createElement(HTML.Tag.TR.toString());
            tableRow.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HWS);
            tableRow.setAttribute(HTML.Attribute.STYLE.toString(), "background-color:rgb(" + r + "," + g + "," + b + ")");
            r -= 10;
            g -= 10;
            b -= 5;
            table.appendChild(tableRow);

            Element name = scheduleDoc.createElement(HTML.Tag.TD.toString());
            name.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HWS);
            name.setTextContent(a.getName());
            tableRow.appendChild(name);

            Element date = scheduleDoc.createElement(HTML.Tag.TD.toString());
            date.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HWS);
            date.setTextContent(a.getDate().toString());
            tableRow.appendChild(date);

            Element third = scheduleDoc.createElement(HTML.Tag.TD.toString());
            third.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_HWS);
            third.setTextContent("TBD");
            tableRow.appendChild(third);
        }
    }

    // SETS UP THE LINKS IN THE NAVBAR AT THE TOP OF THE PAGE
    private void appendNavbarLinks(Document doc, Course courseToExport, CoursePage page) {
        List<CoursePage> pages = courseToExport.getPages();
        int index = pages.indexOf(page);
        Node navbarNode = getNodeWithId(doc, HTML.Tag.DIV.toString(), ID_NAVBAR);
        for (int i = 0; i < pages.size(); i++) {
            Element linkElement = doc.createElement(HTML.Tag.A.toString());
            linkElement.setAttribute(HTML.Attribute.ID.toString(), getID(pages.get(i)));
            linkElement.setAttribute(HTML.Attribute.HREF.toString(), getLink(pages.get(i)));
            if (index == i) {
                linkElement.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_OPEN_NAV);
            } else {
                linkElement.setAttribute(HTML.Attribute.CLASS.toString(), CLASS_NAV);
            }
            linkElement.setTextContent(getDescription(pages.get(i)));
            navbarNode.appendChild(linkElement);
        }
    }

    // FINDS AND RETURNS A NODE IN A DOCUMENT OF A CERTAIN TYPE WITH A CERTIAN ID
    private Node getNodeWithId(Document doc, String tagType, String searchID) {
        NodeList nodes = doc.getElementsByTagName(tagType);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node testNode = nodes.item(i);
            Node testAttr = testNode.getAttributes().getNamedItem(HTML.Attribute.ID.toString());
            if (testAttr.getNodeValue().equals(searchID)) {
                return testNode;
            }
        }
        return null;
    }

    // SAVES THE DOCUMENT OBJECT TO A FILE, WHICH WOULD BE AN HTIM FILE
    private void saveDocument(Document doc, String outputFilePath)
            throws TransformerException, TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        Result result = new StreamResult(new File(outputFilePath));
        Source source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    // SETS THE COURSE PAGE BANNER
    private void setBanner(Document doc, Course courseToExport) {
        // GET THE BANNER NODE
        Node bannerNode = getNodeWithId(doc, HTML.Tag.DIV.toString(), ID_BANNER);

        // APPEND THE FIRST LINE
        String textNode1Text = courseToExport.getSubject().toString() + " " + courseToExport.getNumber()
                + DASH + courseToExport.getSemester().toString() + " " + courseToExport.getYear();
        Text textNode1 = doc.createTextNode(textNode1Text);
        bannerNode.appendChild(textNode1);

        // THEN THE LINE BREAK
        Node brNode = doc.createElement(HTML.Tag.BR.toString());
        bannerNode.appendChild(brNode);

        // AND THEN THE SECOND LINE
        Text textNode2 = doc.createTextNode(courseToExport.getTitle());
        bannerNode.appendChild(textNode2);
    }

    // USED FOR GETTING THE PAGE LINKS FOR PAGE LINKS IN THE NAVBAR
    private String getLink(CoursePage page) {
        if (page == CoursePage.INDEX) {
            return INDEX_PAGE;
        } else if (page == CoursePage.SYLLABUS) {
            return SYLLABUS_PAGE;
        } else if (page == CoursePage.SCHEDULE) {
            return SCHEDULE_PAGE;
        } else if (page == CoursePage.HWS) {
            return HWS_PAGE;
        } else {
            return PROJECTS_PAGE;
        }
    }

    // USED FOR GETTING THE TEXT FOR PAGE LINKS IN THE NAVBAR
    private String getDescription(CoursePage page) {
        if (page == CoursePage.INDEX) {
            return INDEX_HEADER;
        } else if (page == CoursePage.SYLLABUS) {
            return SYLLABUS_HEADER;
        } else if (page == CoursePage.SCHEDULE) {
            return SCHEDULE_HEADER;
        } else if (page == CoursePage.HWS) {
            return HWS_HEADER;
        } else {
            return PROJECTS_HEADER;
        }
    }

    // USED FOR GETTING IDs FOR PAGE LINKS IN THE NAVBAR
    private String getID(CoursePage page) {
        if (page == CoursePage.INDEX) {
            return ID_HOME_LINK;
        } else if (page == CoursePage.SYLLABUS) {
            return ID_SYLLABUS_LINK;
        } else if (page == CoursePage.SCHEDULE) {
            return ID_SCHEDULE_LINK;
        } else if (page == CoursePage.HWS) {
            return ID_HWS_LINK;
        } else {
            return ID_PROJECTS_LINK;
        }
    }
}
