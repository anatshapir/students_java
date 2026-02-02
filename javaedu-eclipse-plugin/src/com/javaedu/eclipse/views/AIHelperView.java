package com.javaedu.eclipse.views;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.javaedu.eclipse.model.AIResponse;
import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.services.ApiClient;
import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;

/**
 * View for AI-powered tutoring assistance.
 */
public class AIHelperView extends ViewPart {

    public static final String ID = "com.javaedu.eclipse.views.AIHelperView";

    private StyledText conversationText;
    private Text questionInput;
    private Button askButton;
    private Label remainingLabel;
    private Button includeCodeCheckbox;

    @Override
    public void createPartControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // Header with remaining requests
        Composite headerComposite = new Composite(container, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Label headerLabel = new Label(headerComposite, SWT.NONE);
        headerLabel.setText("AI Tutor - Ask questions about your code");

        remainingLabel = new Label(headerComposite, SWT.NONE);
        remainingLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        remainingLabel.setText("Requests remaining: --");

        // Conversation display
        Group conversationGroup = new Group(container, SWT.NONE);
        conversationGroup.setText("Conversation");
        conversationGroup.setLayout(new GridLayout(1, false));
        conversationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        conversationText = new StyledText(conversationGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        conversationText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        conversationText.setText("Welcome! I'm your Java programming tutor. I can help you understand concepts, " +
                "debug issues, and guide you through problem-solving.\n\n" +
                "Note: I won't give you direct answers, but I'll help you learn!\n\n" +
                "---\n\n");

        // Input section
        Group inputGroup = new Group(container, SWT.NONE);
        inputGroup.setText("Ask a Question");
        inputGroup.setLayout(new GridLayout(2, false));
        inputGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        // Include code checkbox
        includeCodeCheckbox = new Button(inputGroup, SWT.CHECK);
        includeCodeCheckbox.setText("Include my current code in the question");
        includeCodeCheckbox.setSelection(true);
        GridData checkboxData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        includeCodeCheckbox.setLayoutData(checkboxData);

        // Question input
        questionInput = new Text(inputGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputData = new GridData(SWT.FILL, SWT.FILL, true, true);
        inputData.heightHint = 60;
        questionInput.setLayoutData(inputData);
        questionInput.setMessage("Type your question here...");

        // Ask button
        askButton = new Button(inputGroup, SWT.PUSH);
        askButton.setText("Ask");
        GridData buttonData = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
        buttonData.widthHint = 80;
        askButton.setLayoutData(buttonData);
        askButton.addListener(SWT.Selection, e -> askQuestion());

        // Enter key to submit
        questionInput.addListener(SWT.KeyDown, e -> {
            if (e.keyCode == SWT.CR && (e.stateMask & SWT.CTRL) != 0) {
                askQuestion();
            }
        });

        // Feedback buttons
        Composite feedbackComposite = new Composite(container, SWT.NONE);
        feedbackComposite.setLayout(new GridLayout(3, false));
        feedbackComposite.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        new Label(feedbackComposite, SWT.NONE).setText("Was the last response helpful?");

        Button helpfulButton = new Button(feedbackComposite, SWT.PUSH);
        helpfulButton.setText("Yes 👍");
        helpfulButton.addListener(SWT.Selection, e -> submitFeedback(true));

        Button notHelpfulButton = new Button(feedbackComposite, SWT.PUSH);
        notHelpfulButton.setText("No 👎");
        notHelpfulButton.addListener(SWT.Selection, e -> submitFeedback(false));
    }

    private void askQuestion() {
        if (!AuthManager.getInstance().isLoggedIn()) {
            appendToConversation("System", "Please login to use the AI helper.");
            return;
        }

        String question = questionInput.getText().trim();
        if (question.isEmpty()) {
            return;
        }

        // Get current code if checkbox is selected
        String currentCode = null;
        if (includeCodeCheckbox.getSelection()) {
            currentCode = getCurrentEditorCode();
        }

        // Get current exercise
        Exercise currentExercise = ExerciseManager.getInstance().getCurrentExercise();
        Long exerciseId = currentExercise != null ? currentExercise.getId() : null;

        // Disable input while waiting
        askButton.setEnabled(false);
        questionInput.setEnabled(false);

        // Show user question
        appendToConversation("You", question);
        questionInput.setText("");

        // Call API asynchronously
        final String codeToSend = currentCode;
        final Long exerciseIdFinal = exerciseId;

        new Thread(() -> {
            try {
                AIResponse response = ApiClient.getInstance().askAIHelper(exerciseIdFinal, question, codeToSend);

                Display.getDefault().asyncExec(() -> {
                    appendToConversation("Tutor", response.getResponse());
                    remainingLabel.setText("Requests remaining: " + response.getRemainingRequests());
                    askButton.setEnabled(true);
                    questionInput.setEnabled(true);
                    questionInput.setFocus();
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    appendToConversation("System", "Error: " + e.getMessage());
                    askButton.setEnabled(true);
                    questionInput.setEnabled(true);
                });
            }
        }).start();
    }

    private void appendToConversation(String sender, String message) {
        String formatted = "[" + sender + "]\n" + message + "\n\n---\n\n";
        conversationText.append(formatted);
        conversationText.setTopIndex(conversationText.getLineCount() - 1);
    }

    private String getCurrentEditorCode() {
        try {
            IEditorPart editor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage()
                    .getActiveEditor();

            if (editor instanceof ITextEditor) {
                ITextEditor textEditor = (ITextEditor) editor;
                IDocument document = textEditor.getDocumentProvider()
                        .getDocument(textEditor.getEditorInput());
                if (document != null) {
                    return document.get();
                }
            }
        } catch (Exception e) {
            // Ignore errors getting editor content
        }
        return null;
    }

    private void submitFeedback(boolean helpful) {
        // This would send feedback to the server
        appendToConversation("System", "Thank you for your feedback!");
    }

    @Override
    public void setFocus() {
        questionInput.setFocus();
    }
}
