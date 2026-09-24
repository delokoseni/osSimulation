package io.github.delokoseni.simulation;

import lombok.Getter;

@Getter
public class Simulation {

    private final OperatingSystem operatingSystem;

    public Simulation(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public SimulationResult run(int maxTime) {

        while (!operatingSystem.isFinished()
                && operatingSystem.getCurrentTime() < maxTime) {

            operatingSystem.tick();
        }

        return createResult();
    }

    private SimulationResult createResult() {
        // TODO: реализовать расчет статистики

        return new SimulationResult(
                operatingSystem.getCurrentTime(),
                operatingSystem.getCompletedTasks().size(),
                0.0,
                0.0,
                0.0
        );
    }
}