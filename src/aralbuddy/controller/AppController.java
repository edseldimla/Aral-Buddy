package aralbuddy.controller;

import aralbuddy.model.User;
import aralbuddy.service.AppService;
import aralbuddy.service.FileStore;
import aralbuddy.view.MainFrame;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

/** Connects Swing events to background service work and refreshes the active screen. */
public final class AppController {
    private final MainFrame frame;
    private AppService service;
    private int currentUserId;
    private boolean busy;

    /** Creates the controller for the one application window. */
    public AppController(MainFrame frame) { this.frame = frame; }

    /** Loads files and first-run password hashes away from the Swing event thread. */
    public void start(Path directory) {
        background("Opening your workspace...", () -> {
            FileStore store = new FileStore(directory);
            try { return new AppService(store); }
            catch (Exception exception) { store.close(); throw exception; }
        }, loaded -> { service = loaded; frame.showLogin(this); }, true);
    }

    /** Exposes business operations to screens without giving them file-handling code. */
    public AppService service() { return service; }

    /** Resolves the latest account after edits, rollback, or tutor promotion. */
    public User currentUser() { return service.user(currentUserId); }

    /** Gives service calls the stable ID of the authenticated account. */
    public int actor() { return currentUserId; }

    /** Authenticates in a worker because password hashing is intentionally expensive. */
    public void login(String email, String password) {
        background("Checking your account...", () -> service.login(email, password), user -> {
            currentUserId = user.getId(); frame.showWorkspace(this);
        }, false);
    }

    /** Saves a complete mutation and only then updates screens and reports success. */
    public void save(String message, Runnable operation, Runnable afterSave) {
        save(message, operation, afterSave, null);
    }

    /** Also blocks a modal form during its background save, leaving it open on error. */
    public void save(String message, Runnable operation, Runnable afterSave, JDialog dialog) {
        if (busy) return;
        if (dialog != null) dialog.setEnabled(false);
        background("Saving changes...", () -> service.commit(() -> { operation.run(); return null; }), ignored -> {
            if (dialog != null) { dialog.setEnabled(true); dialog.dispose(); }
            if (afterSave != null) afterSave.run();
            frame.refreshCurrentPage();
            frame.showStatus(message);
        }, false, dialog);
    }

    /** Gives tutor queue responses their actual result message. */
    public void respond(boolean accept) {
        background("Saving your response...", () -> service.commit(() -> service.respondToNext(currentUserId, accept)), message -> {
            frame.refreshCurrentPage(); frame.showStatus(message);
            JOptionPane.showMessageDialog(frame, message, "Tutor request", JOptionPane.INFORMATION_MESSAGE);
        }, false);
    }

    /** Confirms logout while preserving all already-saved work. */
    public void logout() {
        if (busy) return;
        if (JOptionPane.showConfirmDialog(frame, "Log out of this account? All successful changes are saved.",
                "Log out", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            currentUserId = 0; frame.showLogin(this);
        }
    }

    /** Prevents closing during a save and releases the file lock before disposal. */
    public void close() {
        if (busy) { JOptionPane.showMessageDialog(frame, "Please wait for the current operation to finish."); return; }
        try { if (service != null) service.close(); frame.dispose(); }
        catch (IOException exception) { showError(exception, frame); }
    }

    /** Displays validation failures without losing form values. */
    public void guard(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException exception) { showError(exception, frame); }
    }

    private <T> void background(String label, Callable<T> task, Consumer<T> done, boolean fatal) {
        background(label, task, done, fatal, null);
    }

    private <T> void background(String label, Callable<T> task, Consumer<T> done, boolean fatal, JDialog dialog) {
        if (busy) return;
        busy = true; frame.setBusy(true, label);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false; frame.setBusy(false, "Ready");
                try { done.accept(get()); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); showError(exception, dialog == null ? frame : dialog); }
                catch (ExecutionException exception) { failed(exception.getCause()); }
                catch (RuntimeException exception) { failed(exception); }
            }
            private void failed(Throwable exception) {
                if (dialog != null) dialog.setEnabled(true);
                showError(exception, dialog == null ? frame : dialog);
                frame.showStatus("The operation was not completed. Your previous data was kept.");
                if (fatal) frame.dispose();
            }
        }.execute();
    }

    private void showError(Throwable exception, Window parent) {
        if (!(exception instanceof IllegalArgumentException)) exception.printStackTrace();
        String message = exception instanceof IllegalArgumentException || exception instanceof IOException
                ? exception.getMessage() : "The operation could not finish. Please try again. Details are in the console.";
        JOptionPane.showMessageDialog(parent, message, "Aral Buddy", JOptionPane.ERROR_MESSAGE);
    }
}
