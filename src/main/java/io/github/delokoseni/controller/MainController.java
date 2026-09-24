package io.github.delokoseni.controller;

import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML
    private TextArea outputArea;

    @FXML
    private Spinner<Integer> blocksSpinner;

    @FXML
    private Spinner<Integer> tactsSpinner;

    @FXML
    private Spinner<Integer> ramSpinner;

    @FXML
    private TextField packageNameField;

    @FXML
    private Label packageTypeLabel;

    @FXML
    private Label cpuTaskCountLabel;

    @FXML
    private Label ioTaskCountLabel;

    private TaskPackage currentPackage;


    @FXML
    public void initialize() {

        blocksSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 64, 10
                )
        );

        tactsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 1000, 100
                )
        );

        ramSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 128, 8
                )
        );

        packageNameField.setText("Пакет не выбран");

        packageTypeLabel.setText("-");
        cpuTaskCountLabel.setText("0");
        ioTaskCountLabel.setText("0");
    }


    @FXML
    private void choosePackage() {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Выбрать пакет");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "JSON files", "*.json"
                )
        );

        File file = fileChooser.showOpenDialog(
                packageNameField.getScene().getWindow()
        );

        if (file != null) {

            packageNameField.setText(file.getName());

            // TODO:
            // Здесь позже загрузим TaskPackage из JSON
        }
    }


    @FXML
    private void createPackage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/io/github/delokoseni/package-creation.fxml"
                    )
            );
            Stage stage = new Stage();
            stage.initOwner(packageNameField.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Создание пакета");
            Scene scene = new Scene(loader.load(), 620, 720);
            scene.getStylesheets().add(
                    getClass().getResource(
                            "/io/github/delokoseni/styles.css"
                    ).toExternalForm()
            );
            stage.setScene(scene);

            PackageCreationController controller = loader.getController();
            controller.setOnPackageSaved(this::setCurrentPackage);
            stage.showAndWait();
        } catch (IOException exception) {
            showInformation(
                    "Ошибка",
                    "Не удалось открыть окно создания пакета: "
                            + exception.getMessage()
            );
        }
    }

    private void setCurrentPackage(String name, TaskPackage taskPackage) {
        currentPackage = taskPackage;
        packageNameField.setText(name);
        int cpuTasks = (int) taskPackage.getTasks().stream()
                .filter(task -> task.getType() == TaskType.CPU_BOUND)
                .count();
        int ioTasks = taskPackage.getTaskCount() - cpuTasks;

        cpuTaskCountLabel.setText(String.valueOf(cpuTasks));
        ioTaskCountLabel.setText(String.valueOf(ioTasks));
        packageTypeLabel.setText(
                cpuTasks == ioTasks
                        ? "Сбалансированный"
                        : cpuTasks > ioTasks
                        ? "Вычислительный"
                        : "Ввод/вывод"
        );
    }


    @FXML
    private void startSimulation() {

        outputArea.clear();

        outputArea.appendText(
                "Запуск моделирования...\n\n"
        );

        int blocks = blocksSpinner.getValue();
        int tacts = tactsSpinner.getValue();
        int ram = ramSpinner.getValue();

        outputArea.appendText(
                "Разделов памяти: " + blocks + "\n"
        );

        outputArea.appendText(
                "Максимальное количество тактов: "
                        + tacts + "\n"
        );

        outputArea.appendText(
                "RAM: " + ram + " ГБ\n\n"
        );

        // TODO:
        // Здесь создадим OperatingSystem
        // и запустим Simulation.

        outputArea.appendText(
                "Моделирование пока не реализовано.\n"
        );
    }


    private void showInformation(
            String title,
            String message
    ) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}