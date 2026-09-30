package io.github.delokoseni.controller;

import io.github.delokoseni.model.Task;
import io.github.delokoseni.model.TaskState;
import io.github.delokoseni.model.TaskType;
import io.github.delokoseni.simulation.SimulationSnapshot;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

final class SimulationCharts {

    private static final Color WAITING_COLOR = Color.web("#CBD5E1");
    private static final Color MATH_COLOR = Color.web("#3B82F6");
    private static final Color IO_COLOR = Color.web("#F59E0B");
    private static final Color COMPLETED_COLOR = Color.web("#22C55E");
    private static final Color IDLE_COLOR = Color.web("#94A3B8");
    private static final Color CPU_ACTIVE_COLOR = Color.web("#22C55E");
    private static final Color IO_ACTIVE_COLOR = Color.web("#3B82F6");
    private static final Color LOAD_UNLOAD_COLOR = Color.web("#A855F7");

    private SimulationCharts() {
    }

    static void initialize(StackPane... slots) {
        String[] messages = {
                "Диаграмма состояний задач появится после запуска",
                "Загрузка CPU и I/O по тактам"
        };

        for (int index = 0; index < slots.length; index++) {
            Label placeholder = new Label(messages[index]);
            placeholder.getStyleClass().add("chart-placeholder");
            slots[index].getChildren().setAll(placeholder);
        }
    }

    static void render(
            StackPane ganttSlot,
            StackPane resourceTimelineSlot,
            List<SimulationSnapshot> snapshots,
            List<Task> tasks
    ) {
        if (snapshots.isEmpty()) {
            initialize(
                    ganttSlot,
                    resourceTimelineSlot
            );
            return;
        }

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
                resourceTimelineSlot,
                "Загрузка CPU и I/O по тактам",
                "Фактические операции и управление жизненным циклом задач по тактам",
                resourceTimeline(snapshots),
                legend(
                        new LegendEntry("Вычисления CPU (MATH)", CPU_ACTIVE_COLOR),
                        new LegendEntry("Операция I/O", IO_ACTIVE_COLOR),
                        new LegendEntry("Загрузка/выгрузка", LOAD_UNLOAD_COLOR),
                        new LegendEntry("Простой", IDLE_COLOR)
                )
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

    private static Node resourceTimeline(List<SimulationSnapshot> snapshots) {
        ResizableCanvas canvas = new ResizableCanvas(
                105,
                (graphics, width, height) -> drawResourceTimeline(
                        graphics,
                        width,
                        height,
                        snapshots
                )
        );
        double usefulCpuUtilization = utilizationPercent(
                snapshots,
                SimulationSnapshot::cpuActive
        );
        // Lifecycle work and computation can occur in the same tact; count elapsed tacts once.
        double cpuTotalUtilization = utilizationPercent(
                snapshots,
                snapshot -> snapshot.cpuActive() || snapshot.loadUnloadActive()
        );
        double ioUtilization = utilizationPercent(
                snapshots,
                SimulationSnapshot::ioActive
        );
        Label cpuUtilizationLabel = utilizationLabel(
                "Useful CPU",
                usefulCpuUtilization
        );
        Label cpuTotalUtilizationLabel = utilizationLabel(
                "Total CPU",
                cpuTotalUtilization
        );
        Label ioUtilizationLabel = utilizationLabel(
                "I/O",
                ioUtilization
        );

        VBox timeline = new VBox(
                2,
                canvas,
                cpuUtilizationLabel,
                cpuTotalUtilizationLabel,
                ioUtilizationLabel
        );
        timeline.setMinSize(0, 0);
        timeline.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(canvas, Priority.ALWAYS);
        return timeline;
    }

    private static double utilizationPercent(
            List<SimulationSnapshot> snapshots,
            Predicate<SimulationSnapshot> isBusy
    ) {
        // Each snapshot is one tact; divide busy tacts by all simulated tacts.
        return 100.0 * snapshots.stream()
                .filter(isBusy)
                .count() / snapshots.size();
    }

    private static Label utilizationLabel(String resource, double utilization) {
        Label label = new Label(String.format(
                Locale.ROOT,
                "%s utilization: %.1f%%",
                resource,
                utilization
        ));
        label.getStyleClass().add("chart-subtitle");
        return label;
    }

    private static void drawResourceTimeline(
            GraphicsContext graphics,
            double width,
            double height,
            List<SimulationSnapshot> snapshots
    ) {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        double left = 38;
        double right = 8;
        double firstTrackTop = 12;
        double trackHeight = 22;
        double trackGap = 20;
        double plotWidth = Math.max(1, width - left - right);
        double cellWidth = plotWidth / snapshots.size();
        double secondTrackTop = firstTrackTop + trackHeight + trackGap;

        graphics.setFont(Font.font(9));
        graphics.setTextAlign(TextAlignment.RIGHT);
        graphics.setFill(Color.web("#344054"));
        graphics.fillText("CPU", left - 6, firstTrackTop + trackHeight * 0.68);
        graphics.fillText("I/O", left - 6, secondTrackTop + trackHeight * 0.68);

        for (int index = 0; index < snapshots.size(); index++) {
            SimulationSnapshot snapshot = snapshots.get(index);
            double x = left + index * cellWidth;
            graphics.setFill(colorFor(cpuTimelineState(snapshot)));
            graphics.fillRect(x, firstTrackTop, cellWidth, trackHeight);
            graphics.setFill(colorFor(ioTimelineState(snapshot)));
            graphics.fillRect(x, secondTrackTop, cellWidth, trackHeight);
        }

        graphics.setStroke(Color.web("#667085"));
        graphics.setLineWidth(0.7);
        graphics.strokeRect(left, firstTrackTop, plotWidth, trackHeight);
        graphics.strokeRect(left, secondTrackTop, plotWidth, trackHeight);

        graphics.setFill(Color.web("#475467"));
        graphics.setTextAlign(TextAlignment.CENTER);
        int tickCount = snapshots.size();
        int labelStep = Math.max(1, (int) Math.ceil(tickCount / 6.0));
        for (int tact = 1; tact <= tickCount; tact += labelStep) {
            double x = left + (tact - 0.5) * cellWidth;
            graphics.fillText(
                    Integer.toString(snapshots.get(tact - 1).tact()),
                    x,
                    secondTrackTop + trackHeight + 15
            );
        }
        if (tickCount > 1 && (tickCount - 1) % labelStep != 0) {
            graphics.fillText(
                    Integer.toString(snapshots.get(tickCount - 1).tact()),
                    left + (tickCount - 0.5) * cellWidth,
                    secondTrackTop + trackHeight + 15
            );
        }
    }

    private static TimelineState cpuTimelineState(
            SimulationSnapshot snapshot
    ) {
        // Execution is the tact's visible CPU state when it coincides with lifecycle events.
        if (snapshot.cpuActive()) {
            return TimelineState.MATH;
        }
        if (snapshot.loadUnloadActive()) {
            return TimelineState.LOAD_UNLOAD;
        }
        return TimelineState.IDLE;
    }

    private static TimelineState ioTimelineState(
            SimulationSnapshot snapshot
    ) {
        return snapshot.ioActive() ? TimelineState.IO : TimelineState.IDLE;
    }

    private static Color colorFor(TimelineState state) {
        return switch (state) {
            case MATH -> CPU_ACTIVE_COLOR;
            case IO -> IO_ACTIVE_COLOR;
            case LOAD_UNLOAD -> LOAD_UNLOAD_COLOR;
            case IDLE -> IDLE_COLOR;
        };
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

    private record LegendEntry(String label, Color color) {
    }

    private enum TimelineState {
        MATH,
        IO,
        LOAD_UNLOAD,
        IDLE
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
