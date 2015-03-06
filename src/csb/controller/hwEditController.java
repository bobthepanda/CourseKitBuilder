package csb.controller;

import static csb.CSB_PropertyType.REMOVE_ITEM_MESSAGE;
import csb.data.Course;
import csb.data.CourseDataManager;
import csb.data.Assignment;
import csb.gui.CSB_GUI;
import csb.gui.MessageDialog;
import csb.gui.HwDialog;
import csb.gui.YesNoCancelDialog;
import javafx.stage.Stage;
import properties_manager.PropertiesManager;

/**
 *
 * @author Henry Chin
 */
public class hwEditController {
    HwDialog hwd;
    MessageDialog messageDialog;
    YesNoCancelDialog yesNoCancelDialog;
    
    public hwEditController(Stage initPrimaryStage, Course course, MessageDialog initMessageDialog, YesNoCancelDialog initYesNoCancelDialog) {
        hwd = new HwDialog(initPrimaryStage, course, initMessageDialog);
        messageDialog = initMessageDialog;
        yesNoCancelDialog = initYesNoCancelDialog;
    }

    // THESE ARE FOR HW ITEMS
    
    public void handleAddHwRequest(CSB_GUI gui) {
        CourseDataManager cdm = gui.getDataManager();
        Course course = cdm.getCourse();
        hwd.showAddHwDialog(course.getStartingMonday());
        
        // DID THE USER CONFIRM?
        if (hwd.wasCompleteSelected()) {
            // GET THE SCHEDULE ITEM
            Assignment hw = hwd.getAssignment();
            
            // AND ADD IT AS A ROW TO THE TABLE
            course.addAssignment(hw);
        }
        else {
            // THE USER MUST HAVE PRESSED CANCEL, SO
            // WE DO NOTHING
        }
    }
    
    public void handleEditHwRequest(CSB_GUI gui, Assignment hw) {
        CourseDataManager cdm = gui.getDataManager();
        Course course = cdm.getCourse();
        hwd.showEditHwDialog(hw);
        
        // DID THE USER CONFIRM?
        if (hwd.wasCompleteSelected()) {
            // UPDATE THE SCHEDULE ITEM
            Assignment newHW = hwd.getAssignment();
            hw.setName(newHW.getName());
            hw.setDate(newHW.getDate());
            hw.setTopics(newHW.getTopics());
        }
        else {
            // THE USER MUST HAVE PRESSED CANCEL, SO
            // WE DO NOTHING
        }        
    }
    
    public void handleRemoveHwRequest(CSB_GUI gui, Assignment hw) {
        // PROMPT THE USER TO SAVE UNSAVED WORK
        yesNoCancelDialog.show(PropertiesManager.getPropertiesManager().getProperty(REMOVE_ITEM_MESSAGE));
        
        // AND NOW GET THE USER'S SELECTION
        String selection = yesNoCancelDialog.getSelection();

        // IF THE USER SAID YES, THEN SAVE BEFORE MOVING ON
        if (selection.equals(YesNoCancelDialog.YES)) { 
            gui.getDataManager().getCourse().removeAssignment(hw);
        }
    }
}