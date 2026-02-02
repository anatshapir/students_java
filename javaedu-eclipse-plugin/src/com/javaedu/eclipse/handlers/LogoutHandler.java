package com.javaedu.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;

/**
 * Handler for the logout command.
 */
public class LogoutHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        if (!AuthManager.getInstance().isLoggedIn()) {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
            messageBox.setText("Not Logged In");
            messageBox.setMessage("You are not currently logged in.");
            messageBox.open();
            return null;
        }

        MessageBox confirmBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirmBox.setText("Confirm Logout");
        confirmBox.setMessage("Are you sure you want to logout?");

        if (confirmBox.open() == SWT.YES) {
            AuthManager.getInstance().logout();
            ExerciseManager.getInstance().clearCache();

            MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
            messageBox.setText("Logged Out");
            messageBox.setMessage("You have been logged out successfully.");
            messageBox.open();
        }

        return null;
    }
}
