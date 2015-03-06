package csb.controller;

import static csb.CSB_PropertyType.REMOVE_ITEM_MESSAGE;
import csb.data.Course;
import csb.data.CourseDataManager;
import csb.data.Lecture;
import csb.gui.CSB_GUI;
import csb.gui.MessageDialog;
import csb.gui.LectureDialog;
import csb.gui.YesNoCancelDialog;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.stage.Stage;
import properties_manager.PropertiesManager;

/**
 *
 * @author Henry Chin
 */
public class LectureEditController {

    LectureDialog ld;
    MessageDialog messageDialog;
    YesNoCancelDialog yesNoCancelDialog;

    public LectureEditController(Stage initPrimaryStage, Course course, MessageDialog initMessageDialog, YesNoCancelDialog initYesNoCancelDialog) {
        ld = new LectureDialog(initPrimaryStage, course, initMessageDialog);
        messageDialog = initMessageDialog;
        yesNoCancelDialog = initYesNoCancelDialog;
    }

    // THESE ARE FOR LECTURES
    public void handleAddLectureRequest(CSB_GUI gui) {
        CourseDataManager cdm = gui.getDataManager();
        Course course = cdm.getCourse();
        ld.showAddLectureDialog();

        // DID THE USER CONFIRM?
        if (ld.wasCompleteSelected()) {
            // GET THE LECTURE
            Lecture l = ld.getLecture();

            // AND ADD IT AS A ROW TO THE TABLE
            course.addLecture(l);
        } else {
            // THE USER MUST HAVE PRESSED CANCEL, SO
            // WE DO NOTHING
        }
    }

    public void handleEditLectureRequest(CSB_GUI gui, Lecture lectureToEdit) {
        CourseDataManager cdm = gui.getDataManager();
        Course course = cdm.getCourse();
        ld.showEditLectureDialog(lectureToEdit);

        // DID THE USER CONFIRM?
        if (ld.wasCompleteSelected()) {
            // UPDATE THE LECTURE
            Lecture l = ld.getLecture();
            lectureToEdit.setTopic(l.getTopic());
            lectureToEdit.setSessions(l.getSessions());
        } else {
            // THE USER MUST HAVE PRESSED CANCEL, SO
            // WE DO NOTHING
        }
    }

    public void handleRemoveLectureRequest(CSB_GUI gui, Lecture lectureToRemove) {
        // PROMPT THE USER TO SAVE UNSAVED WORK
        yesNoCancelDialog.show(PropertiesManager.getPropertiesManager().getProperty(REMOVE_ITEM_MESSAGE));

        // AND NOW GET THE USER'S SELECTION
        String selection = yesNoCancelDialog.getSelection();

        // IF THE USER SAID YES, THEN SAVE BEFORE MOVING ON
        if (selection.equals(YesNoCancelDialog.YES)) {
            gui.getDataManager().getCourse().removeLecture(lectureToRemove);
        }
    }

    //MOVES LECTURES UP IN THE LIST
    public void handleUpRequest(CSB_GUI gui, TableViewSelectionModel<Lecture> list, Lecture lectureToMove) {
        ObservableList<Lecture> lectures = gui.getDataManager().getCourse().getLectures();
        int index = lectures.indexOf(lectureToMove);
        if (index != 0) {
            //MOVES IT UP IN THE LIST
            Lecture pre = lectures.get(index - 1);
            lectures.set(index, pre);
            lectures.set(index - 1, lectureToMove);
            //MOVES THE POSITION OF THE SELECTED INDEX TO THE NEW ONE
            list.clearAndSelect(index-1);
        }
    }

    //MOVES LECTURES DOWN IN THE LIST
    public void handleDownRequest(CSB_GUI gui, TableViewSelectionModel<Lecture> list, Lecture lectureToMove) {
        ObservableList<Lecture> lectures = gui.getDataManager().getCourse().getLectures();
        int index = lectures.indexOf(lectureToMove);
        if (index != lectures.size() - 1) {
            //MOVES IT DOWN IN THE LIST
            Lecture post = lectures.get(index + 1);
            lectures.set(index, post);
            lectures.set(index + 1, lectureToMove);
            //MOVES THE POSITION OF THE SELECTED INDEX TO THE NEW ONE
            list.clearAndSelect(index+1);
        }
    }
}
