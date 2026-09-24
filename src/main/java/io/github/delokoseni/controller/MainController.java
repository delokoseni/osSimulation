package io.github.delokoseni.controller;

import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.simulation.TaskGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

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

        // Пока просто проверяем, что кнопка работает.
        showInformation(
                "Создание пакета",
                "Здесь будет окно создания нового пакета."
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