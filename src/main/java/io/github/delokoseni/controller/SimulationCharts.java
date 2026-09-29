package io.github.delokoseni.controller;

import io.github.delokoseni.model.CpuState;
import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskPackage;
import io.github.delokoseni.model.TaskState;
import io.github.delokoseni.model.TaskType;
import io.github.delokoseni.simulation.Simulation;
import io.github.delokoseni.simulation.SimulationSnapshot;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

final class SimulationCharts {

    private static final Color WAITING_COLOR = Color.web("#CBD5E1");
    private static final Color MATH_COLOR = Color.web("#3B82F6");
    private static final Color IO_COLOR = Color.web("#F59E0B");
    private static final Color COMPLETED_COLOR = Color.web("#22C55E");
    private static final Color IDLE_COLOR = Color.web("#94A3B8");
    private static final Color IO_WAIT_COLOR = Color.web("#F59E0B");
    private static final Color OVERLOADED_COLOR = Color.web("#EF4444");

    private SimulationCharts() {
    }

    static void initialize(StackPane... slots) {
        String[] messages = {
                "Диаграмма состояний задач появится после запуска",
                "Состояние процессора по тактам",
                "Активность I/O-задач по тактам",
                "Занятость разделов памяти",
                "Ход выполнения пакета",
                "Среднее время оборота задач",
                "Производительность при разном составе пакета"
        };

        for (int index = 0; index < slots.length; index++) {
            Label placeholder = new Label(messages[index]);
            placeholder.getStyleClass().add("chart-placeholder");
            slots[index].getChildren().setAll(placeholder);
        }
    }

    static List<RatioPerformance> runRatioStudy(
            TaskPackage sourcePackage,
            int maxBlocksCount,
            int ram,
            int maxTacts
    ) {
        List<Task> sourceTasks = sourcePackage.getTasks();
        if (sourceTasks.isEmpty()) {
            return List.of();
        }

        int taskCount = sourceTasks.size();
        TreeSet<Integer> mathTaskCounts = new TreeSet<>();
        for (int quarter = 0; quarter <= 4; quarter++) {
            mathTaskCounts.add((int) Math.round(taskCount * quarter / 4.0));
        }

        List<RatioPerformance> results = new ArrayList<>();
        for (int mathTaskCount : mathTaskCounts) {
            List<Task> tasks = new ArrayList<>(taskCount);
            for (int index = 0; index < taskCount; index++) {
                boolean isMath = ((long) (index + 1) * mathTaskCount) / taskCount
                        > ((long) index * mathTaskCount) / taskCount;
                tasks.add(new Task(
                        index + 1,
                        isMath ? TaskType.CPU_BOUND : TaskType.IO_BOUND,
                        sourceTasks.get(index).getMemoryRequired(),
                        0
                ));
            }

            Simulation trial = new Simulation(
                    new TaskPackage(tasks),
                    maxBlocksCount,
                    ram,
                    maxTacts
            );
            trial.start();

            List<SimulationSnapshot> snapshots =
                    trial.getOperatingSystem().getSnapshots();
            int completedTasks = snapshots.isEmpty()
                    ? 0
                    : snapshots.get(snapshots.size() - 1).completedTasks();
            double throughput = trial.getTotalTacts() == 0
                    ? 0
                    : (double) completedTasks / trial.getTotalTacts();

            results.add(new RatioPerformance(
                    100.0 * mathTaskCount / taskCount,
                    throughput
            ));
        }

        return List.copyOf(results);
    }

    static void render(
            StackPane ganttSlot,
            StackPane cpuSlot,
            StackPane ioSlot,
            StackPane memorySlot,
            StackPane completionSlot,
            StackPane turnaroundSlot,
            StackPane ratioSlot,
            List<SimulationSnapshot> snapshots,
            List<Task> tasks,
            List<RatioPerformance> ratioPerformance
    ) {
        if (snapshots.isEmpty()) {
            initialize(
                    ganttSlot,
                    cpuSlot,
                    ioSlot,
                    memorySlot,
                    completionSlot,
                    turnaroundSlot,
                    ratioSlot
            );
            return;
        }

        int totalTasks = tasks.size();
        SimulationSnapshot last = snapshots.get(snapshots.size() - 1);
        double throughput =
                (double) last.completedTasks() / snapshots.size();
        double cpuIdlePercent = 100.0 * snapshots.stream()
                .filter(snapshot ->
                        snapshot.cpuState() == CpuState.IDLE
                                || snapshot.cpuState() == CpuState.IO_WAIT
                )
                .count() / snapshots.size();
        double cpuBusyPercent = 100.0 * snapshots.stream()
                .filter(snapshot -> snapshot.cpuState() == CpuState.EXECUTING)
                .count() / snapshots.size();
        long cpuIdleTicks = snapshots.stream()
                .filter(snapshot ->
                        snapshot.cpuState() == CpuState.IDLE
                                || snapshot.cpuState() == CpuState.IO_WAIT
                )
                .count();
        double overloadedPercent = 100.0 * snapshots.stream()
                .filter(snapshot -> snapshot.cpuState() == CpuState.OVERLOADED)
                .count() / snapshots.size();

        setCard(
                ganttSlot,
                "Задачи во времени",
                "Ожидание → выполнение → завершение",
                ganttChart(snapshots, tasks),
                legend(
                        new LegendEntry("Ожидание", WAITING_COLOR),
                        new LegendEntry("MATH", MATH_COLOR),
                        new LegendEntry("I/O", IO_COLOR),
                        new LegendEntry("Завершено", COMPLETED_COLOR)
                )
        );
        setCard(
                cpuSlot,
                "Состояние CPU по тактам",
                String.format(
                        "Вычисления: %.1f%% · простой, включая ожидание I/O: %d/%d тактов (%.1f%%) · перегрузка: %.1f%%",
                        cpuBusyPercent,
                        cpuIdleTicks,
                        snapshots.size(),
                        cpuIdlePercent,
                        overloadedPercent
                ),
                cpuTimeline(snapshots),
                legend(
                        new LegendEntry("Вычисления", MATH_COLOR),
                        new LegendEntry("Ожидание I/O", IO_WAIT_COLOR),
                        new LegendEntry("Простой", IDLE_COLOR),
                        new LegendEntry("Перегрузка", OVERLOADED_COLOR)
                )
        );

        LineChart<Number, Number> ioChart =
                lineChart("Такт", "Активные I/O-задачи", false);
        XYChart.Series<Number, Number> ioSeries =
                new XYChart.Series<>();
        ioSeries.setName("I/O-bound");
        for (SimulationSnapshot snapshot : snapshots) {
            ioSeries.getData().add(new XYChart.Data<>(
                    snapshot.tact(),
                    snapshot.ioTasksRunning()
            ));
        }
        ioChart.getData().add(ioSeries);
        setCard(
                ioSlot,
                "Активность I/O-задач",
                "Число выполняющихся I/O-bound задач на каждом такте",
                ioChart,
                null
        );

        LineChart<Number, Number> memoryChart =
                lineChart("Такт", "Число разделов", true);
        XYChart.Series<Number, Number> usedSeries =
                new XYChart.Series<>();
        usedSeries.setName("Занято");
        XYChart.Series<Number, Number> capacitySeries =
                new XYChart.Series<>();
        capacitySeries.setName("Доступно");
        for (SimulationSnapshot snapshot : snapshots) {
            usedSeries.getData().add(new XYChart.Data<>(
                    snapshot.tact(),
                    snapshot.usedBlocks()
            ));
            capacitySeries.getData().add(new XYChart.Data<>(
                    snapshot.tact(),
                    snapshot.maxBlocksCount()
            ));
        }
        memoryChart.getData().addAll(usedSeries, capacitySeries);
        setCard(
                memorySlot,
                "Использование разделов памяти",
                "Сравнение занятых разделов с текущей емкостью",
                memoryChart,
                null
        );

        LineChart<Number, Number> completionChart =
                lineChart("Такт", "Число задач", true);
        XYChart.Series<Number, Number> completedSeries =
                new XYChart.Series<>();
        completedSeries.setName("Завершено");
        XYChart.Series<Number, Number> remainingSeries =
                new XYChart.Series<>();
        remainingSeries.setName("Осталось");
        for (SimulationSnapshot snapshot : snapshots) {
            completedSeries.getData().add(new XYChart.Data<>(
                    snapshot.tact(),
                    snapshot.completedTasks()
            ));
            remainingSeries.getData().add(new XYChart.Data<>(
                    snapshot.tact(),
                    totalTasks - snapshot.completedTasks()
            ));
        }
        completionChart.getData().addAll(completedSeries, remainingSeries);
        setCard(
                completionSlot,
                "Завершение пакета",
                String.format(
                        "Производительность: %.2f задач/такт · выполнено %d из %d",
                        throughput,
                        last.completedTasks(),
                        totalTasks
                ),
                completionChart,
                null
        );

        setCard(
                turnaroundSlot,
                "Среднее время оборота",
                "От поступления пакета до завершения; незавершенные задачи не учитываются",
                turnaroundChart(snapshots, tasks),
                null
        );
        setCard(
                ratioSlot,
                "Производительность при разном составе пакета",
                "Повторные запуски той же модели при разных долях MATH (50/50 — сбалансированный пакет)",
                ratioChart(ratioPerformance),
                null
        );
    }

    private static Node ganttChart(
            List<SimulationSnapshot> snapshots,
            List<Task> tasks
    ) {
        Map<Integer, Map<Integer, SimulationSnapshot.TaskProgress>> progress =
                new HashMap<>();
        for (SimulationSnapshot snapshot : snapshots) {
            Map<Integer, SimulationSnapshot.TaskProgress> tickProgress =
                    new HashMap<>();
            for (SimulationSnapshot.TaskProgress task : snapshot.taskProgress()) {
                tickProgress.put(task.id(), task);
            }
            progress.put(snapshot.tact(), tickProgress);
        }

        double canvasHeight = Math.max(160, tasks.size() * 18.0 + 45);
        ResizableCanvas canvas = new ResizableCanvas(
                canvasHeight,
                (graphics, width, height) -> drawGantt(
                        graphics,
                        width,
                        height,
                        snapshots,
                        tasks,
                        progress
                )
        );
        ScrollPane scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(155);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    private static void drawGantt(
            GraphicsContext graphics,
            double width,
            double height,
            List<SimulationSnapshot> snapshots,
            List<Task> tasks,
            Map<Integer, Map<Integer, SimulationSnapshot.TaskProgress>> progress
    ) {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        double left = 67;
        double right = 8;
        double top = 8;
        double bottom = 23;
        double plotWidth = Math.max(1, width - left - right);
        double rowHeight = Math.max(1, (height - top - bottom) / Math.max(1, tasks.size()));
        double cellWidth = plotWidth / snapshots.size();

        graphics.setFont(Font.font(9));
        for (int row = 0; row < tasks.size(); row++) {
            Task task = tasks.get(row);
            double y = top + row * rowHeight;
            graphics.setFill(Color.web("#344054"));
            graphics.setTextAlign(TextAlignment.RIGHT);
            graphics.fillText(
                    "Задача " + task.getId(),
                    left - 6,
                    y + rowHeight * 0.68
            );

            for (int index = 0; index < snapshots.size(); index++) {
                int tact = snapshots.get(index).tact();
                SimulationSnapshot.TaskProgress taskProgress =
                        progress.getOrDefault(tact, Map.of()).get(task.getId());
                Color color = taskProgress == null
                        ? WAITING_COLOR
                        : taskColor(taskProgress);
                graphics.setFill(color);
                graphics.fillRect(
                        left + index * cellWidth + 0.5,
                        y + 1,
                        Math.max(1, cellWidth - 1),
                        Math.max(1, rowHeight - 2)
                );
            }
        }

        graphics.setStroke(Color.web("#98A2B3"));
        graphics.setLineWidth(0.6);
        graphics.strokeLine(left, height - bottom, width - right, height - bottom);
        graphics.setFill(Color.web("#475467"));
        graphics.setTextAlign(TextAlignment.CENTER);
        int tickCount = snapshots.size();
        int labelStep = Math.max(1, (int) Math.ceil(tickCount / 6.0));
        for (int tact = 1; tact <= tickCount; tact += labelStep) {
            double x = left + (tact - 0.5) * cellWidth;
            graphics.fillText(Integer.toString(tact), x, height - 7);
        }
        if (tickCount > 1 && (tickCount - 1) % labelStep != 0) {
            graphics.fillText(
                    Integer.toString(tickCount),
                    left + (tickCount - 0.5) * cellWidth,
                    height - 7
            );
        }
    }

    private static Color taskColor(SimulationSnapshot.TaskProgress task) {
        return switch (task.state()) {
            case WAITING -> WAITING_COLOR;
            case RUNNING -> task.type() == TaskType.CPU_BOUND
                    ? MATH_COLOR
                    : IO_COLOR;
            case READY, TERMINATED -> COMPLETED_COLOR;
            case NEW -> IDLE_COLOR;
        };
    }

    private static Node cpuTimeline(List<SimulationSnapshot> snapshots) {
        ResizableCanvas canvas = new ResizableCanvas(
                85,
                (graphics, width, height) -> drawCpuTimeline(
                        graphics,
                        width,
                        height,
                        snapshots
                )
        );
        VBox.setVgrow(canvas, Priority.ALWAYS);
        return canvas;
    }

    private static void drawCpuTimeline(
            GraphicsContext graphics,
            double width,
            double height,
            List<SimulationSnapshot> snapshots
    ) {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        double left = 8;
        double right = 8;
        double top = 16;
        double trackHeight = 28;
        double plotWidth = Math.max(1, width - left - right);
        double cellWidth = plotWidth / snapshots.size();

        for (int index = 0; index < snapshots.size(); index++) {
            graphics.setFill(cpuStateColor(snapshots.get(index).cpuState()));
            graphics.fillRect(
                    left + index * cellWidth,
                    top,
                    Math.max(1, cellWidth),
                    trackHeight
            );
        }

        graphics.setStroke(Color.web("#667085"));
        graphics.setLineWidth(0.7);
        graphics.strokeRect(left, top, plotWidth, trackHeight);

        graphics.setFill(Color.web("#475467"));
        graphics.setFont(Font.font(9));
        graphics.setTextAlign(TextAlignment.CENTER);
        int tickCount = snapshots.size();
        int labelStep = Math.max(1, (int) Math.ceil(tickCount / 6.0));
        for (int tact = 1; tact <= tickCount; tact += labelStep) {
            double x = left + (tact - 0.5) * cellWidth;
            graphics.fillText(Integer.toString(tact), x, top + trackHeight + 15);
        }
        if (tickCount > 1 && (tickCount - 1) % labelStep != 0) {
            graphics.fillText(
                    Integer.toString(tickCount),
                    left + (tickCount - 0.5) * cellWidth,
                    top + trackHeight + 15
            );
        }
    }

    private static Color cpuStateColor(CpuState state) {
        return switch (state) {
            case IDLE -> IDLE_COLOR;
            case IO_WAIT -> IO_WAIT_COLOR;
            case EXECUTING -> MATH_COLOR;
            case OVERLOADED -> OVERLOADED_COLOR;
        };
    }

    private static LineChart<Number, Number> lineChart(
            String xLabel,
            String yLabel,
            boolean legend
    ) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);
        xAxis.setForceZeroInRange(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setForceZeroInRange(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(legend);
        chart.setMinHeight(160);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private static Node turnaroundChart(
            List<SimulationSnapshot> snapshots,
            List<Task> tasks
    ) {
        Map<Integer, Integer> completionTacts = new HashMap<>();
        for (SimulationSnapshot snapshot : snapshots) {
            for (SimulationSnapshot.TaskProgress task : snapshot.taskProgress()) {
                if (task.state() == TaskState.READY) {
                    completionTacts.putIfAbsent(task.id(), snapshot.tact());
                }
            }
        }

        Map<TaskType, List<Integer>> turnaroundByType =
                new EnumMap<>(TaskType.class);
        for (Task task : tasks) {
            Integer completionTact = completionTacts.get(task.getId());
            if (completionTact != null) {
                turnaroundByType
                        .computeIfAbsent(task.getType(), unused -> new ArrayList<>())
                        .add(Math.max(0, completionTact - task.getArrivalTime()));
            }
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Тип задачи");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Среднее время (такты)");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setMinHeight(160);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        addAverageTurnaround(series, turnaroundByType, TaskType.CPU_BOUND, "MATH");
        addAverageTurnaround(series, turnaroundByType, TaskType.IO_BOUND, "I/O");
        chart.getData().add(series);
        return chart;
    }

    private static void addAverageTurnaround(
            XYChart.Series<String, Number> series,
            Map<TaskType, List<Integer>> turnaroundByType,
            TaskType type,
            String label
    ) {
        List<Integer> values = turnaroundByType.get(type);
        if (values != null && !values.isEmpty()) {
            double average = values.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
            series.getData().add(new XYChart.Data<>(label, average));
        }
    }

    private static void setCard(
            StackPane slot,
            String title,
            String subtitle,
            Node chart,
            Node legend
    ) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chart-title");

        VBox card = new VBox(3);
        card.getChildren().add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("chart-subtitle");
            card.getChildren().add(subtitleLabel);
        }
        if (legend != null) {
            card.getChildren().add(legend);
        }
        card.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        slot.getChildren().setAll(card);
    }

    private static HBox legend(LegendEntry... entries) {
        HBox legend = new HBox(7);
        for (LegendEntry entry : entries) {
            Rectangle marker = new Rectangle(9, 9, entry.color());
            Label label = new Label(entry.label());
            label.getStyleClass().add("chart-legend-label");
            HBox item = new HBox(3, marker, label);
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            legend.getChildren().add(item);
        }
        return legend;
    }

    private static LineChart<Number, Number> ratioChart(
            List<RatioPerformance> ratios
    ) {
        NumberAxis xAxis = new NumberAxis(0, 100, 25);
        xAxis.setLabel("MATH-задачи в пакете (%)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Задач/такт");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);
        chart.setMinHeight(160);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Производительность");
        for (RatioPerformance ratio : ratios) {
            series.getData().add(new XYChart.Data<>(
                    ratio.mathTaskPercent(),
                    ratio.throughput()
            ));
        }
        chart.getData().add(series);
        return chart;
    }

    private record LegendEntry(String label, Color color) {
    }

    record RatioPerformance(double mathTaskPercent, double throughput) {
    }

    private static final class ResizableCanvas extends Region {

        private final Canvas canvas = new Canvas();
        private final CanvasPainter painter;
        private final double preferredHeight;

        private ResizableCanvas(
                double preferredHeight,
                CanvasPainter painter
        ) {
            this.preferredHeight = preferredHeight;
            this.painter = painter;
            getChildren().add(canvas);
            setMinSize(0, 0);
            setPrefHeight(preferredHeight);
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        @Override
        protected void layoutChildren() {
            double width = Math.max(0, getWidth());
            double height = Math.max(0, getHeight());
            canvas.setWidth(width);
            canvas.setHeight(height);
            canvas.relocate(0, 0);
            painter.paint(canvas.getGraphicsContext2D(), width, height);
        }

        @Override
        protected double computePrefWidth(double height) {
            return 300;
        }

        @Override
        protected double computePrefHeight(double width) {
            return preferredHeight;
        }
    }

    @FunctionalInterface
    private interface CanvasPainter {
        void paint(GraphicsContext graphics, double width, double height);
    }
}
