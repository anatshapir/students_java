package com.javaedu.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.model.Submission;
import com.javaedu.eclipse.services.ApiClient;
import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;

/**
 * Handler for submitting code.
 */
public class SubmitHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Check if logged in
        if (!AuthManager.getInstance().isLoggedIn()) {
            MessageDialog.openError(shell, "Not Logged In",
                    "Please login before submitting code.");
            return null;
        }

        // Check if exercise is selected
        Exercise currentExercise = ExerciseManager.getInstance().getCurrentExercise();
        if (currentExercise == null) {
            MessageDialog.openError(shell, "No Exercise Selected",
                    "Please select an exercise from the Exercise List view first.");
            return null;
        }

        // Get code from active editor
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        String code = getEditorCode(editor);

        if (code == null || code.trim().isEmpty()) {
            MessageDialog.openError(shell, "No Code",
                    "Please write some code before submitting.");
            return null;
        }

        // Confirm submission
        boolean confirm = MessageDialog.openConfirm(shell, "Submit Solution",
                "Submit your solution for \"" + currentExercise.getTitle() + "\"?\n\n" +
                "Your code will be compiled and tested on the server.");

        if (!confirm) {
            return null;
        }

        // Submit with progress dialog
        final String codeToSubmit = code;

        try {
            new ProgressMonitorDialog(shell).run(true, false, monitor -> {
                monitor.beginTask("Submitting solution...", IProgressMonitor.UNKNOWN);

                try {
                    // Submit code
                    Submission submission = ApiClient.getInstance()
                            .submitCode(currentExercise.getId(), codeToSubmit);

                    // Poll for results
                    int attempts = 0;
                    while (attempts < 30 && isPending(submission.getStatus())) {
                        Thread.sleep(2000);
                        submission = ApiClient.getInstance().getSubmission(submission.getId());
                        attempts++;
                        monitor.setTaskName("Running tests... (" + attempts * 2 + "s)");
                    }

                    final Submission finalSubmission = submission;

                    // Show results
                    shell.getDisplay().asyncExec(() -> showResults(shell, finalSubmission));

                } catch (Exception e) {
                    shell.getDisplay().asyncExec(() ->
                        MessageDialog.openError(shell, "Submission Error",
                                "Failed to submit: " + e.getMessage())
                    );
                }
            });
        } catch (Exception e) {
            MessageDialog.openError(shell, "Error", "Submission failed: " + e.getMessage());
        }

        return null;
    }

    private String getEditorCode(IEditorPart editor) {
        if (editor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) editor;
            IDocument document = textEditor.getDocumentProvider()
                    .getDocument(textEditor.getEditorInput());
            if (document != null) {
                return document.get();
            }
        }
        return null;
    }

    private boolean isPending(String status) {
        return "PENDING".equals(status) || "COMPILING".equals(status) || "RUNNING".equals(status);
    }

    private void showResults(Shell shell, Submission submission) {
        StringBuilder message = new StringBuilder();
        message.append("Status: ").append(submission.getStatus()).append("\n\n");

        if (submission.getGrade() != null) {
            message.append("Score: ").append(submission.getGrade().getScore())
                   .append("/").append(submission.getGrade().getMaxScore())
                   .append(" (").append(String.format("%.1f%%", submission.getGrade().getPercentage()))
                   .append(")\n\n");

            if (submission.getGrade().getFeedback() != null) {
                message.append("Feedback:\n").append(submission.getGrade().getFeedback());
            }
        } else if ("COMPILATION_ERROR".equals(submission.getStatus())) {
            message.append("Compilation Error:\n");
            message.append(submission.getCompilerOutput() != null ?
                    submission.getCompilerOutput() : "Unknown error");
        } else {
            message.append("Results pending...");
        }

        int icon = submission.getGrade() != null && submission.getGrade().getPercentage() >= 70
                ? MessageDialog.INFORMATION
                : MessageDialog.WARNING;

        MessageDialog dialog = new MessageDialog(shell,
                "Submission Results",
                null,
                message.toString(),
                icon,
                new String[]{"OK"},
                0);
        dialog.open();
    }

    // Inner interface for compatibility
    interface IProgressMonitor {
        int UNKNOWN = -1;
        void beginTask(String name, int totalWork);
        void setTaskName(String name);
    }
}
