package com.javaedu.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;

/**
 * Handler for running tests locally.
 */
public class RunTestsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Check if logged in
        if (!AuthManager.getInstance().isLoggedIn()) {
            MessageDialog.openError(shell, "Not Logged In",
                    "Please login to access exercises.");
            return null;
        }

        // Check if exercise is selected
        Exercise currentExercise = ExerciseManager.getInstance().getCurrentExercise();
        if (currentExercise == null) {
            MessageDialog.openError(shell, "No Exercise Selected",
                    "Please select an exercise from the Exercise List view first.");
            return null;
        }

        // For local test running, we would need to:
        // 1. Get the visible test cases from the exercise
        // 2. Compile the student's code locally
        // 3. Run the tests and show results

        // For now, show a message that this feature requires the Eclipse JDT
        MessageDialog.openInformation(shell, "Run Tests Locally",
                "Local test execution is being set up.\n\n" +
                "In the meantime, you can use 'Submit Solution' to run tests on the server.\n\n" +
                "Exercise: " + currentExercise.getTitle() + "\n" +
                "Available visible tests: " +
                    (currentExercise.getTestCases() != null ?
                            currentExercise.getTestCases().stream()
                                    .filter(tc -> !tc.isHidden())
                                    .count() : 0));

        return null;
    }
}
