package com.javaedu.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;
import com.javaedu.eclipse.views.AIHelperView;

/**
 * Handler for asking for hints.
 */
public class AskHintHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Check if logged in
        if (!AuthManager.getInstance().isLoggedIn()) {
            MessageDialog.openError(shell, "Not Logged In",
                    "Please login to use the hint feature.");
            return null;
        }

        // Check if exercise is selected
        Exercise currentExercise = ExerciseManager.getInstance().getCurrentExercise();
        if (currentExercise == null) {
            MessageDialog.openError(shell, "No Exercise Selected",
                    "Please select an exercise from the Exercise List view first.");
            return null;
        }

        // Open the AI Helper view
        try {
            IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
            page.showView(AIHelperView.ID);
        } catch (PartInitException e) {
            MessageDialog.openError(shell, "Error",
                    "Could not open AI Helper view: " + e.getMessage());
        }

        return null;
    }
}
