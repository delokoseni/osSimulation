package io.github.delokoseni.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
        tasksTable.setRowFactory(table -> {
            TableRow<Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    tasks.remove(row.getItem());
                    renumberTasks();
                    refreshTable();
                }
            });
            return row;
        });
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
        int memory = memorySpinner.getValue();
        tasks.clear();
        for (int index = 1; index <= taskCount; index++) {
            TaskType type = index % 2 == 0
                    ? TaskType.IO_BOUND
                    : TaskType.CPU_BOUND;
            tasks.add(createTask(index, type, memory));
        }
        refreshTable();
    }

    @FXML
    private void createInOutPackage() {
        createAutomaticPackage(TaskType.IO_BOUND);
    }

    @FXML
    private void addTask() {
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
        if (tasks.isEmpty()) {
            showError("Добавьте хотя бы одну задачу.");
            return;
        }

        Path targetPath;
        try {
            targetPath = saveAsJson(name);
            if (onPackageSaved != null) {
                onPackageSaved.accept(name, new TaskPackage(tasks));
            }
            showInformation(
                    "Пакет сохранен",
                    "Пакет успешно сохранен.\nФайл: "
                            + targetPath
                            + "\nРазмер: " + Files.size(targetPath) + " байт"
            );
            close();
        } catch (IOException | InvalidPathException exception) {
            showError(
                    "Пакет не сохранен.\nФайл: " + getDisplayPath(name)
                            + "\nПричина: " + exception.getMessage()
            );
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void createAutomaticPackage(TaskType type) {
        int taskCount = taskCountSpinner.getValue();
        int memory = memorySpinner.getValue();
        tasks.clear();
        for (int index = 1; index <= taskCount; index++) {
            tasks.add(createTask(index, type, memory));
        }
        refreshTable();
    }

    private Task createTask(int number, TaskType type, int memory) {
        return new Task(number, type, memory, 0);
    }

    private void refreshTable() {
        tasksTable.setItems(FXCollections.observableArrayList(tasks));
    }

    private void renumberTasks() {
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).setId(index + 1);
        }
    }

    private Path saveAsJson(String packageName) throws IOException {
        if (packageName.contains("\\")
                || packageName.contains("/")
                || packageName.matches(".*[<>:\"|?*].*")) {
            throw new InvalidPathException(packageName, "Недопустимое имя файла");
        }

        String fileName = targetFileName(packageName);
        Path directory = Path.of(System.getProperty("user.dir"))
                .resolve("packeges")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(directory);
        Path file = directory.resolve(fileName);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode taskNodes = root.putArray("tasks");
        for (Task task : tasks) {
            ObjectNode taskNode = taskNodes.addObject();
            taskNode.put("num", task.getId());
            taskNode.put(
                    "type",
                    task.getType() == TaskType.CPU_BOUND ? "MATH" : "INOUT"
            );
            taskNode.put("memory", task.getMemoryRequired());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        if (!Files.isRegularFile(file) || Files.size(file) == 0) {
            throw new IOException("Файл не был создан или оказался пустым");
        }
        return file;
    }

    private String getDisplayPath(String packageName) {
        Path directory = Path.of(System.getProperty("user.dir"))
                .resolve("packeges")
                .toAbsolutePath()
                .normalize();
        if (packageName.isBlank()) {
            return directory.toString();
        }
        try {
            return directory.resolve(targetFileName(packageName))
                    .toAbsolutePath()
                    .normalize()
                    .toString();
        } catch (InvalidPathException exception) {
            return directory.toString();
        }
    }

    private String targetFileName(String packageName) {
        return packageName.endsWith(".json")
                ? packageName
                : packageName + ".json";
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

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
