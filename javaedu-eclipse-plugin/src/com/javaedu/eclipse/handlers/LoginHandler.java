package com.javaedu.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;

import com.javaedu.eclipse.services.ApiClient;
import com.javaedu.eclipse.services.AuthManager;

/**
 * Handler for the login command.
 */
public class LoginHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        if (AuthManager.getInstance().isLoggedIn()) {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
            messageBox.setText("Already Logged In");
            messageBox.setMessage("You are already logged in as " + AuthManager.getInstance().getUserEmail());
            messageBox.open();
            return null;
        }

        LoginDialog dialog = new LoginDialog(shell);
        dialog.open();

        return null;
    }

    /**
     * Login dialog.
     */
    private static class LoginDialog extends TitleAreaDialog {
        private Text emailText;
        private Text passwordText;
        private Text nameText;
        private Button loginButton;
        private Button registerButton;
        private boolean isRegisterMode = false;

        public LoginDialog(Shell parentShell) {
            super(parentShell);
        }

        @Override
        public void create() {
            super.create();
            setTitle("Login to JavaEdu");
            setMessage("Enter your credentials to access JavaEdu");
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite area = (Composite) super.createDialogArea(parent);
            Composite container = new Composite(area, SWT.NONE);
            container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            container.setLayout(new GridLayout(2, false));

            // Name (only for registration)
            Label nameLabel = new Label(container, SWT.NONE);
            nameLabel.setText("Name:");
            nameLabel.setVisible(false);
            nameText = new Text(container, SWT.BORDER);
            nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            nameText.setVisible(false);

            // Email
            Label emailLabel = new Label(container, SWT.NONE);
            emailLabel.setText("Email:");
            emailText = new Text(container, SWT.BORDER);
            emailText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            // Password
            Label passwordLabel = new Label(container, SWT.NONE);
            passwordLabel.setText("Password:");
            passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
            passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            // Toggle link
            Link toggleLink = new Link(container, SWT.NONE);
            toggleLink.setText("<a>New user? Register here</a>");
            GridData linkData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
            toggleLink.setLayoutData(linkData);
            toggleLink.addListener(SWT.Selection, e -> {
                isRegisterMode = !isRegisterMode;
                nameLabel.setVisible(isRegisterMode);
                nameText.setVisible(isRegisterMode);
                if (isRegisterMode) {
                    toggleLink.setText("<a>Already have an account? Login</a>");
                    setTitle("Register for JavaEdu");
                    loginButton.setText("Register");
                } else {
                    toggleLink.setText("<a>New user? Register here</a>");
                    setTitle("Login to JavaEdu");
                    loginButton.setText("Login");
                }
                container.layout();
            });

            return area;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            loginButton = createButton(parent, IDialogConstants.OK_ID, "Login", true);
            createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
        }

        @Override
        protected void okPressed() {
            String email = emailText.getText().trim();
            String password = passwordText.getText();

            if (email.isEmpty() || password.isEmpty()) {
                setErrorMessage("Please enter email and password");
                return;
            }

            if (isRegisterMode && nameText.getText().trim().isEmpty()) {
                setErrorMessage("Please enter your name");
                return;
            }

            try {
                ApiClient.LoginResponse response;
                if (isRegisterMode) {
                    response = ApiClient.getInstance().register(nameText.getText().trim(), email, password);
                } else {
                    response = ApiClient.getInstance().login(email, password);
                }

                AuthManager.getInstance().login(
                        response.getAccessToken(),
                        response.getRefreshToken(),
                        response.getUserId(),
                        response.getEmail(),
                        response.getName()
                );

                MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                messageBox.setText("Success");
                messageBox.setMessage("Welcome, " + response.getName() + "!");
                messageBox.open();

                super.okPressed();

            } catch (Exception e) {
                setErrorMessage("Login failed: " + e.getMessage());
            }
        }
    }
}
