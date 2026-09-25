package io.github.delokoseni.controller;

import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskPackageType;
import io.github.delokoseni.model.TaskType;
import io.github.delokoseni.simulation.OperatingSystem;
import io.github.delokoseni.simulation.Simulation;
import io.github.delokoseni.simulation.SimulationResult;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

    /**
     * Текущий выбранный пакет задач.
     */
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

    /**
     * Выбор готового JSON-пакета.
     */
    @FXML
    private void choosePackage() {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Выбрать пакет");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "JSON files",
                        "*.json"
                )
        );

        File file = fileChooser.showOpenDialog(
                packageNameField.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        try {

            TaskPackageType packet =
                    new TaskPackageType(file.toPath());

            currentPackage =
                    new TaskPackage(packet.getTasks());

            packageNameField.setText(file.getName());

            updatePackageInfo(packet);

        } catch (IOException e) {

            currentPackage = null;

            packageNameField.setText("Пакет не выбран");

            packageTypeLabel.setText("Ошибка");
            cpuTaskCountLabel.setText("-");
            ioTaskCountLabel.setText("-");

            showInformation(
                    "Ошибка",
                    "Не удалось загрузить пакет:\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /**
     * Открывает окно создания нового пакета.
     */
    @FXML
    private void createPackage() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/io/github/delokoseni/package-creation.fxml"
                    )
            );

            Stage stage = new Stage();

            stage.initOwner(
                    packageNameField.getScene().getWindow()
            );

            stage.initModality(
                    Modality.WINDOW_MODAL
            );

            stage.setTitle("Создание пакета");

            Scene scene = new Scene(
                    loader.load(),
                    620,
                    720
            );

            scene.getStylesheets().add(
                    getClass().getResource(
                            "/io/github/delokoseni/styles.css"
                    ).toExternalForm()
            );

            stage.setScene(scene);

            PackageCreationController controller =
                    loader.getController();

            controller.setOnPackageSaved(
                    this::setCurrentPackage
            );

            stage.showAndWait();

        } catch (IOException exception) {

            showInformation(
                    "Ошибка",
                    "Не удалось открыть окно создания пакета:\n"
                            + exception.getMessage()
            );
        }
    }

    /**
     * Получение пакета из окна его создания.
     */
    private void setCurrentPackage(
            String name,
            TaskPackage taskPackage
    ) {

        currentPackage = taskPackage;

        packageNameField.setText(name);

        int cpuTasks = (int) taskPackage.getTasks()
                .stream()
                .filter(task ->
                        task.getType() == TaskType.CPU_BOUND
                )
                .count();

        int ioTasks =
                taskPackage.getTaskCount() - cpuTasks;

        cpuTaskCountLabel.setText(
                String.valueOf(cpuTasks)
        );

        ioTaskCountLabel.setText(
                String.valueOf(ioTasks)
        );

        packageTypeLabel.setText(
                cpuTasks == ioTasks
                        ? "Сбалансированный"
                        : cpuTasks > ioTasks
                        ? "Вычислительный"
                        : "Ввод/вывод"
        );
    }

    /**
     * Запуск моделирования.
     */
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
                "Разделов памяти: "
                        + blocks
                        + "\n"
        );

        outputArea.appendText(
                "Максимальное количество тактов: "
                        + tacts
                        + "\n"
        );

        outputArea.appendText(
                "RAM: "
                        + ram
                        + " ГБ\n\n"
        );

        if (currentPackage == null) {

            showInformation(
                    "Ошибка",
                    "Сначала выберите пакет заданий."
            );

            return;
        }

        try {

            /*
             * Simulation сама создаст OperatingSystem.
             */
            Simulation simulation =
                    new Simulation(
                            currentPackage,
                            blocks,
                            ram,
                            tacts
                    );

            /*
             * Передаем вывод симуляции в TextArea.
             */
            simulation.getOperatingSystem()
                    .setOutputCallback(
                            message -> outputArea.appendText(
                                    message + "\n"
                            )
                    );

            /*
             * Запускаем моделирование.
             */
            simulation.start();

            /*
             * Выводим время работы программы.
             */
            outputArea.appendText(
                    "\nВремя работы программы: "
                            + String.format(
                            "%.4f",
                            simulation.getRunTime()
                    )
                            + " секунд\n"
            );

            /*
             * Фактическое количество выполненных тактов.
             */
            outputArea.appendText(
                    "Выполнено тактов: "
                            + simulation.getTotalTacts()
                            + "\n"
            );

        } catch (Exception e) {

            showInformation(
                    "Ошибка",
                    "Не удалось запустить симуляцию:\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /**
     * Вывод информации о пакете.
     */
    private void updatePackageInfo(
            TaskPackageType packet
    ) {

        if (packet == null ||
                packet.getTasksCount() == 0) {

            packageTypeLabel.setText("-");
            cpuTaskCountLabel.setText("0");
            ioTaskCountLabel.setText("0");

            return;
        }

        packageTypeLabel.setText(
                packet.getType().getDisplayName()
        );

        cpuTaskCountLabel.setText(
                String.valueOf(
                        packet.getMathTasks()
                )
        );

        ioTaskCountLabel.setText(
                String.valueOf(
                        packet.getInOutTasks()
                )
        );
    }

    /**
     * Показ информационного сообщения.
     */
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