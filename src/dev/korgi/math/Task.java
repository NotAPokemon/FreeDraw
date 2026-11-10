package dev.korgi.math;

import java.util.function.Consumer;

/**
 * Represents a task to be run after a certain amount of time, run on the Time
 * loop
 * 
 * @author Aarush
 */
public class Task {

    private double seconds;
    private Runnable run;
    private Consumer<Task> remove;

    public Task(double seconds, Runnable run, Consumer<Task> remove) {
        this.seconds = seconds;
        this.run = run;
        this.remove = remove;
    }

    public void update(double dt) {
        this.seconds -= dt;
        if (seconds < 0) {
            this.run.run();
            remove.accept(this);
        }
    }

}
