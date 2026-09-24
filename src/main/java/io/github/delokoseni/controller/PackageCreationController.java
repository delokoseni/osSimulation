package io.github.delokoseni.controller;

import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PackageCreationController {

    @FXML
    private TextField packageNameField;

    @FXML
    private Spinner<Integer> taskCountSpinner;

    @FXML
    private ComboBox<String> taskTypeComboBox;

    @FXML
    private Spinner<Integer> memorySpinner;

    @FXML
    private TableView<Task> tasksTable;

    @FXML
    private TableColumn<Task, Integer> numberColumn;

    @FXML
    private TableColumn<Task, String> typeColumn;

    @FXML
    private TableColumn<Task, Integer> memoryColumn;

    private final List<Task> tasks = new ArrayList<>();
    private BiConsumer<String, TaskPackage> onPackageSaved;

    @FXML
    private void initialize() {
        taskCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, Integer.MAX_VALUE, 1
                )
        );
        memorySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, Integer.MAX_VALUE, 1
                )
        );
        taskTypeComboBox.setItems(FXCollections.observableArrayList("Math", "Inout"));
        taskTypeComboBox.getSelectionModel().selectFirst();

        numberColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cell.getValue().getId()
                ).asObject()
        );
        typeColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        displayType(cell.getValue().getType())
                )
        );
        memoryColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cell.getValue().getMemoryRequired()
                ).asObject()
        );
        refreshTable();
    }

    public void setOnPackageSaved(BiConsumer<String, TaskPackage> onPackageSaved) {
        this.onPackageSaved = onPackageSaved;
    }

    @FXML
    private void createMathPackage() {
        createAutomaticPackage(TaskType.CPU_BOUND);
    }

    @FXML
    private void createBalancedPackage() {
        int taskCount = taskCountSpinner.getValue();
        tasks.clear();
        for (int index = 1; index <= taskCount; index++) {
            TaskType type = index % 2 == 0
                    ? TaskType.IO_BOUND
                    : TaskType.CPU_BOUND;
            tasks.add(createTask(index, type, 1));
        }
        refreshTable();
    }

    @FXML
    private void createInOutPackage() {
        createAutomaticPackage(TaskType.IO_BOUND);
    }

    @FXML
    private void addTask() {
        if (tasks.size() >= taskCountSpinner.getValue()) {
            showError("Количество задач уже достигнуто.");
            return;
        }

        TaskType type = "Math".equals(taskTypeComboBox.getValue())
                ? TaskType.CPU_BOUND
                : TaskType.IO_BOUND;
        tasks.add(createTask(tasks.size() + 1, type, memorySpinner.getValue()));
        refreshTable();
    }

    @FXML
    private void savePackage() {
        String name = packageNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Введите имя пакета.");
            return;
        }
        if (tasks.size() != taskCountSpinner.getValue()) {
            showError("Добавьте указанное количество задач.");
            return;
        }

        if (onPackageSaved != null) {
            onPackageSaved.accept(name, new TaskPackage(tasks));
        }
        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void createAutomaticPackage(TaskType type) {
        int taskCount = taskCountSpinner.getValue();
        tasks.clear();
        for (int index = 1; index <= taskCount; index++) {
            tasks.add(createTask(index, type, 1));
        }
        refreshTable();
    }

    private Task createTask(int number, TaskType type, int memory) {
        return new Task(number, type, memory, 0);
    }

    private void refreshTable() {
        tasksTable.setItems(FXCollections.observableArrayList(tasks));
    }

    private String displayType(TaskType type) {
        return type == TaskType.CPU_BOUND ? "Math" : "Inout";
    }

    private void close() {
        ((Stage) packageNameField.getScene().getWindow()).close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Создание пакета");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
