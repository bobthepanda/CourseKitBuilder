package csb.gui;

import csb.CSB_PropertyType;
import csb.data.Course;
import csb.data.Assignment;
import static csb.gui.CSB_GUI.CLASS_HEADING_LABEL;
import static csb.gui.CSB_GUI.CLASS_PROMPT_LABEL;
import static csb.gui.CSB_GUI.PRIMARY_STYLE_SHEET;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
public class HwDialog  extends Stage {
    // THIS IS THE OBJECT DATA BEHIND THIS UI
    Assignment hw;
    
    // GUI CONTROLS FOR OUR DIALOG
    GridPane gridPane;
    Scene dialogScene;
    Label headingLabel;
    Label nameLabel;
    TextField nameTextField;
    Label dateLabel;
    DatePicker datePicker;
    Label topicLabel;
    TextField topicTextField;
    Button completeButton;
    Button cancelButton;
    
    // THIS IS FOR KEEPING TRACK OF WHICH BUTTON THE USER PRESSED
    String selection;
    
    // CONSTANTS FOR OUR UI
    public static final String COMPLETE = "Complete";
    public static final String CANCEL = "Cancel";
    public static final String NAME_PROMPT = "Name: ";
    public static final String DATE_PROMPT = "Due Date: ";
    public static final String TOPICS_PROMPT = "Topics: ";
    public static final String HW_HEADING = "Assignment Details";
    public static final String ADD_HW_TITLE = "Add New Assignments";
    public static final String EDIT_HW_TITLE = "Assignments";
    /**
     * Initializes this dialog so that it can be used for either adding
     * new schedule items or editing existing ones.
     * 
     * @param primaryStage The owner of this modal dialog.
     */
    public HwDialog(Stage primaryStage, Course course,  MessageDialog messageDialog) {       
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
        headingLabel = new Label(HW_HEADING);
        headingLabel.getStyleClass().add(CLASS_HEADING_LABEL);
    
        // NOW THE NAME
        nameLabel = new Label(NAME_PROMPT);
        nameLabel.getStyleClass().add(CLASS_PROMPT_LABEL);
        nameTextField = new TextField();
        nameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            hw.setName(newValue);
        });
        
        // AND THE TOPIC
        topicLabel = new Label(TOPICS_PROMPT);
        topicLabel.getStyleClass().add(CLASS_PROMPT_LABEL);
        topicTextField = new TextField();
        topicTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            hw.setTopics(newValue);
        });
        
        // AND THE DATE
        dateLabel = new Label(DATE_PROMPT);
        dateLabel.getStyleClass().add(CLASS_PROMPT_LABEL);
        datePicker = new DatePicker();
        datePicker.setOnAction(e -> {
            if (datePicker.getValue().isBefore(course.getStartingMonday())
                    || datePicker.getValue().isAfter(course.getEndingFriday())) {
                // INCORRECT SELECTION, NOTIFY THE USER
                PropertiesManager props = PropertiesManager.getPropertiesManager();
                messageDialog.show(props.getProperty(CSB_PropertyType.ILLEGAL_DATE_MESSAGE));
            }             
            else {
                hw.setDate(datePicker.getValue());
            }
        });
        
        // AND FINALLY, THE BUTTONS
        completeButton = new Button(COMPLETE);
        cancelButton = new Button(CANCEL);
        
        // REGISTER EVENT HANDLERS FOR OUR BUTTONS
        EventHandler completeCancelHandler = (EventHandler<ActionEvent>) (ActionEvent ae) -> {
            Button sourceButton = (Button)ae.getSource();
            HwDialog.this.selection = sourceButton.getText();
            HwDialog.this.hide();
        };
        completeButton.setOnAction(completeCancelHandler);
        cancelButton.setOnAction(completeCancelHandler);

        // NOW LET'S ARRANGE THEM ALL AT ONCE
        gridPane.add(headingLabel, 0, 0, 2, 1);
        gridPane.add(nameLabel, 0, 1, 1, 1);
        gridPane.add(nameTextField, 1, 1, 1, 1);
        gridPane.add(topicLabel, 0, 2, 1, 1);
        gridPane.add(topicTextField, 1, 2, 1, 1);
        gridPane.add(dateLabel, 0, 3, 1, 1);
        gridPane.add(datePicker, 1, 3, 1, 1);
        gridPane.add(completeButton, 0, 4, 1, 1);
        gridPane.add(cancelButton, 1, 4, 1, 1);

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
    
    public Assignment getAssignment() { 
        return hw;
    }
    
    /**
     * This method loads a custom message into the label and
     * then pops open the dialog.
     * 
     * @param message Message to appear inside the dialog.
     */
    public Assignment showAddHwDialog(LocalDate initDate) {
        // SET THE DIALOG TITLE
        setTitle(ADD_HW_TITLE);
        
        // RESET THE SCHEDULE ITEM OBJECT WITH DEFAULT VALUES
        hw = new Assignment();
        
        // LOAD THE UI STUFF
        nameTextField.setText(hw.getName());
        datePicker.setValue(initDate);
        topicTextField.setText(hw.getTopics());
        
        // AND OPEN IT UP
        this.showAndWait();
        
        return hw;
    }
    
    public void loadGUIData() {
        // LOAD THE UI STUFF
        nameTextField.setText(hw.getName());
        datePicker.setValue(hw.getDate());
        topicTextField.setText(hw.getTopics());       
    }
    
    public boolean wasCompleteSelected() {
        return selection.equals(COMPLETE);
    }
    
    public void showEditHwDialog(Assignment itemToEdit) {
        // SET THE DIALOG TITLE
        setTitle(EDIT_HW_TITLE);
        
        // LOAD THE SCHEDULE ITEM INTO OUR LOCAL OBJECT
        hw = new Assignment();
        hw.setName(itemToEdit.getName());
        hw.setDate(itemToEdit.getDate());
        hw.setTopics(itemToEdit.getTopics());
        
        // AND THEN INTO OUR GUI
        loadGUIData();
               
        // AND OPEN IT UP
        this.showAndWait();
    }
}