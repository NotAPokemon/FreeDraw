package dev.korgi.math;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds helpful functions relating to time
 * 
 * @author Arvan
 */
public class Time {

    private static final Map<String, Long> lastTime = new HashMap<>();
    private static final Map<String, Long> cd = new HashMap<>();
    private static long startTime;
    private static long lastLoopTime;

    public static final List<Task> todos = new ArrayList<>();
    public static final List<Task> toRemoveTodos = new ArrayList<>();

    private static void createCooldown(String name, double timeSeconds) {
        lastTime.put(name, System.nanoTime());
        cd.put(name, (long) (timeSeconds * 1_000_000_000L));
    }

    public static void applyAfterTime(double seconds, Runnable run) {
        todos.add(new Task(seconds, run, toRemoveTodos::add));
    }

    public static boolean exists(String name) {
        return lastTime.containsKey(name) && cd.containsKey(name);
    }

    public static boolean check(String name) {
        if (!lastTime.containsKey(name) || !cd.containsKey(name))
            return false;

        long elapsed = System.nanoTime() - lastTime.get(name);
        return elapsed > cd.get(name);
    }

    public static double getRemainingTime(String name) {
        if (!lastTime.containsKey(name) || !cd.containsKey(name))
            return Double.NaN;

        long elapsed = System.nanoTime() - lastTime.get(name);
        return cd.get(name) - elapsed;
    }

    public static void cooldown(String name, Runnable action, double timeSeconds) {
        boolean shouldUse = false;

        if (cd.get(name) == null || Math.abs(cd.get(name) - timeSeconds) < 0.001) {
            createCooldown(name, timeSeconds);
            shouldUse = true;
        }
        if (shouldUse || check(name)) {
            lastTime.put(name, System.nanoTime());
            action.run();
        }
    }

    public static void startTimer() {
        startTime = System.nanoTime();
    }

    public static void stopTimer(String output, double threashold) {
        double time = (System.nanoTime() - startTime) / 1e9;
        if (time > threashold)
            System.out.println(output.formatted(time));
    }

    public static double stopTimer() {
        double time = (System.nanoTime() - startTime) / 1e9;
        return time;
    }

    public static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss, d MMM uuuu"));
    }

    public static void loop() {
        double dt = (System.nanoTime() - lastLoopTime) / 1e9;

        for (Task task : todos) {
            task.update(dt);
        }

        for (Task task : toRemoveTodos) {
            todos.remove(task);
        }

        lastLoopTime = System.nanoTime();
    }

}
