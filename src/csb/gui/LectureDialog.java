package csb.gui;

import csb.CSB_PropertyType;
import csb.data.Course;
import csb.data.Lecture;
import static csb.gui.CSB_GUI.CLASS_HEADING_LABEL;
import static csb.gui.CSB_GUI.CLASS_PROMPT_LABEL;
import static csb.gui.CSB_GUI.PRIMARY_STYLE_SHEET;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import properties_manager.PropertiesManager;

/**
 *
 * @author Henry Chin
 */
public class LectureDialog  extends Stage {
    // THIS IS THE OBJECT DATA BEHIND THIS UI
    Lecture lecture;
    
    // GUI CONTROLS FOR OUR DIALOG
    GridPane gridPane;
    Scene dialogScene;
    Label headingLabel;
    Label topicLabel;
    TextField topicTextField;
    Label sessionsLabel;
    ComboBox sessionsBox;
    Button completeButton;
    Button cancelButton;
    
    // THIS IS FOR KEEPING TRACK OF WHICH BUTTON THE USER PRESSED
    String selection;
    
    // CONSTANTS FOR OUR UI
    public static final String COMPLETE = "Complete";
    public static final String CANCEL = "Cancel";
    public static final String TOPIC_PROMPT = "Topic: ";
    public static final String SESSIONS_PROMPT = "Number of Sessions: ";
    public static final String SCHEDULE_ITEM_HEADING = "Lecture Details";
    public static final String ADD_SCHEDULE_ITEM_TITLE = "Add Lecture";
    public static final String EDIT_SCHEDULE_ITEM_TITLE = "Edit Lecture";
    /**
     * Initializes this dialog so that it can be used for either adding
     * new schedule items or editing existing ones.
     * 
     * @param primaryStage The owner of this modal dialog.
     */
    public LectureDialog(Stage primaryStage, Course course,  MessageDialog messageDialog) {       
        // MAKE THIS DIALOG MODAL, MEANING OTHERS WILL WAIT
        // FOR IT WHEN IT IS DISPLAYED
        initModality(Modality.WINDOW_MODAL);
        initOwner(primaryStage);
        
        // FIRST OUR CONTAINER
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 20, 20, 20));
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        
        // PUT THE HEADING IN THE GRID, NOTE THAT THE TEXT WILL DEPEND
        // ON WHETHER WE'RE ADDING OR EDITING
        headingLabel = new Label(SCHEDULE_ITEM_HEADING);
        headingLabel.getStyleClass().add(CLASS_HEADING_LABEL);
    
        // NOW THE DESCRIPTION 
        topicLabel = new Label(TOPIC_PROMPT);
        topicLabel.getStyleClass().add(CLASS_PROMPT_LABEL);
        topicTextField = new TextField();
        topicTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            lecture.setTopic(newValue);
        });
        
        // AND THE DATE
        sessionsLabel = new Label(SESSIONS_PROMPT);
        sessionsLabel.getStyleClass().add(CLASS_PROMPT_LABEL);
        sessionsBox = new ComboBox();
        for (int i = 1; i < 4; i++) {
            sessionsBox.getItems().add(i);
        }
        sessionsBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            lecture.setSessions((int)newValue);
        });
        
        // AND FINALLY, THE BUTTONS
        completeButton = new Button(COMPLETE);
        cancelButton = new Button(CANCEL);
        
        // REGISTER EVENT HANDLERS FOR OUR BUTTONS
        EventHandler completeCancelHandler = (EventHandler<ActionEvent>) (ActionEvent ae) -> {
            Button sourceButton = (Button)ae.getSource();
            LectureDialog.this.selection = sourceButton.getText();
            LectureDialog.this.hide();
        };
        completeButton.setOnAction(completeCancelHandler);
        cancelButton.setOnAction(completeCancelHandler);

        // NOW LET'S ARRANGE THEM ALL AT ONCE
        gridPane.add(headingLabel, 0, 0, 2, 1);
        gridPane.add(topicLabel, 0, 1, 1, 1);
        gridPane.add(topicTextField, 1, 1, 1, 1);
        gridPane.add(sessionsLabel, 0, 2, 1, 1);
        gridPane.add(sessionsBox, 1, 2, 1, 1);
        gridPane.add(completeButton, 0, 3, 1, 1);
        gridPane.add(cancelButton, 1, 3, 1, 1);

        // AND PUT THE GRID PANE IN THE WINDOW
        dialogScene = new Scene(gridPane);
        dialogScene.getStylesheets().add(PRIMARY_STYLE_SHEET);
        this.setScene(dialogScene);
    }
    
    /**
     * Accessor method for getting the selection the user made.
     * 
     * @return Either YES, NO, or CANCEL, depending on which
     * button the user selected when this dialog was presented.
     */
    public String getSelection() {
        return selection;
    }
    
    public Lecture getLecture() { 
        return lecture;
    }
    
    /**
     * This method loads a custom message into the label and
     * then pops open the dialog.
     * 
     * @param message Message to appear inside the dialog.
     */
    public Lecture showAddLectureDialog() {
        // SET THE DIALOG TITLE
        setTitle(ADD_SCHEDULE_ITEM_TITLE);
        
        // RESET THE SCHEDULE ITEM OBJECT WITH DEFAULT VALUES
        lecture = new Lecture();
        
        // LOAD THE UI STUFF
        topicTextField.setText(lecture.getTopic());
        sessionsBox.setValue(lecture.getSessions());
        
        // AND OPEN IT UP
        this.showAndWait();
        
        return lecture;
    }
    
    public void loadGUIData() {
        // LOAD THE UI STUFF
        topicTextField.setText(lecture.getTopic());
        sessionsBox.setValue(lecture.getSessions()); 
    }
    
    public boolean wasCompleteSelected() {
        return selection.equals(COMPLETE);
    }
    
    public void showEditLectureDialog(Lecture itemToEdit) {
        // SET THE DIALOG TITLE
        setTitle(EDIT_SCHEDULE_ITEM_TITLE);
        
        // LOAD THE SCHEDULE ITEM INTO OUR LOCAL OBJECT
        lecture = new Lecture();
        lecture.setTopic(itemToEdit.getTopic());
        lecture.setSessions(itemToEdit.getSessions());
        
        // AND THEN INTO OUR GUI
        loadGUIData();
               
        // AND OPEN IT UP
        this.showAndWait();
    }
}