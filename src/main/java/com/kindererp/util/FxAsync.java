package com.kindererp.util;

import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.stage.Window;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs database work off the JavaFX thread and hands the result back on the JavaFX thread.
 * A single worker thread keeps SQLite access sequential. Failures are shown with {@link Dialogs}.
 */
public final class FxAsync {

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "db-worker");
        thread.setDaemon(true);
        return thread;
    });

    private FxAsync() {
    }

    /** @param busy node disabled (with a wait cursor) while the work runs; may be null */
    public static <T> void run(Node busy, Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        setBusy(busy, true);
        task.setOnSucceeded(e -> {
            setBusy(busy, false);
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(e -> {
            setBusy(busy, false);
            Dialogs.showError(windowOf(busy), task.getException());
        });
        WORKER.submit(task);
    }

    public static void runAction(Node busy, Runnable work, Runnable onSuccess) {
        run(busy, () -> {
            work.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    private static void setBusy(Node node, boolean busy) {
        if (node == null) {
            return;
        }
        node.setDisable(busy);
        if (node.getScene() != null) {
            node.getScene().setCursor(busy ? Cursor.WAIT : Cursor.DEFAULT);
        }
    }

    private static Window windowOf(Node node) {
        return node != null && node.getScene() != null ? node.getScene().getWindow() : null;
    }
}
