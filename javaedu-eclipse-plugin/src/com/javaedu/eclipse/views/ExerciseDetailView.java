package com.javaedu.eclipse.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;

import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.model.Hint;
import com.javaedu.eclipse.services.ExerciseManager;

import java.util.List;

/**
 * View showing exercise details, description, and hints.
 */
public class ExerciseDetailView extends ViewPart {

    public static final String ID = "com.javaedu.eclipse.views.ExerciseDetailView";

    private Exercise currentExercise;
    private Browser descriptionBrowser;
    private StyledText starterCodeText;
    private Composite hintsContainer;
    private Label titleLabel;
    private Label difficultyLabel;
    private Label pointsLabel;
    private Label dueDateLabel;
    private int revealedHints = 0;

    @Override
    public void createPartControl(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.VERTICAL);

        // Top section: Exercise info and description
        createInfoSection(sashForm);

        // Bottom section: Starter code and hints
        createCodeAndHintsSection(sashForm);

        sashForm.setWeights(new int[]{60, 40});
    }

    private void createInfoSection(Composite parent) {
        Composite infoContainer = new Composite(parent, SWT.NONE);
        infoContainer.setLayout(new GridLayout(2, false));

        // Title
        new Label(infoContainer, SWT.NONE).setText("Title:");
        titleLabel = new Label(infoContainer, SWT.NONE);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Difficulty
        new Label(infoContainer, SWT.NONE).setText("Difficulty:");
        difficultyLabel = new Label(infoContainer, SWT.NONE);
        difficultyLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Points
        new Label(infoContainer, SWT.NONE).setText("Points:");
        pointsLabel = new Label(infoContainer, SWT.NONE);
        pointsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Due date
        new Label(infoContainer, SWT.NONE).setText("Due Date:");
        dueDateLabel = new Label(infoContainer, SWT.NONE);
        dueDateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Description (using Browser for markdown rendering)
        Label descLabel = new Label(infoContainer, SWT.NONE);
        descLabel.setText("Description:");
        GridData descLabelData = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
        descLabel.setLayoutData(descLabelData);

        descriptionBrowser = new Browser(infoContainer, SWT.BORDER);
        GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        browserData.heightHint = 200;
        descriptionBrowser.setLayoutData(browserData);
    }

    private void createCodeAndHintsSection(Composite parent) {
        TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

        // Starter Code tab
        TabItem starterCodeTab = new TabItem(tabFolder, SWT.NONE);
        starterCodeTab.setText("Starter Code");

        Composite starterCodeContainer = new Composite(tabFolder, SWT.NONE);
        starterCodeContainer.setLayout(new GridLayout(1, false));

        starterCodeText = new StyledText(starterCodeContainer, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        starterCodeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        starterCodeText.setEditable(false);

        Button copyButton = new Button(starterCodeContainer, SWT.PUSH);
        copyButton.setText("Copy to Clipboard");
        copyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        copyButton.addListener(SWT.Selection, e -> copyStarterCode());

        starterCodeTab.setControl(starterCodeContainer);

        // Hints tab
        TabItem hintsTab = new TabItem(tabFolder, SWT.NONE);
        hintsTab.setText("Hints");

        hintsContainer = new Composite(tabFolder, SWT.NONE);
        hintsContainer.setLayout(new GridLayout(1, false));

        hintsTab.setControl(hintsContainer);
    }

    public void showExercise(Exercise exercise) {
        this.currentExercise = exercise;
        this.revealedHints = 0;

        Display.getDefault().asyncExec(() -> {
            titleLabel.setText(exercise.getTitle());
            difficultyLabel.setText(exercise.getDifficulty());
            pointsLabel.setText(String.valueOf(exercise.getPoints()));
            dueDateLabel.setText(exercise.getDueDate() != null ? exercise.getDueDate() : "No due date");

            // Render description as HTML
            String html = "<html><body style='font-family: Arial, sans-serif; padding: 10px;'>" +
                          exercise.getDescription().replace("\n", "<br>") +
                          "</body></html>";
            descriptionBrowser.setText(html);

            // Show starter code
            starterCodeText.setText(exercise.getStarterCode() != null ? exercise.getStarterCode() : "");

            // Reset hints
            updateHintsDisplay();
        });
    }

    private void updateHintsDisplay() {
        // Clear existing hints
        for (Control child : hintsContainer.getChildren()) {
            child.dispose();
        }

        if (currentExercise == null || currentExercise.getHints() == null) {
            Label noHints = new Label(hintsContainer, SWT.NONE);
            noHints.setText("No hints available for this exercise.");
            hintsContainer.layout();
            return;
        }

        List<Hint> hints = currentExercise.getHints();

        // Show revealed hints
        for (int i = 0; i < revealedHints && i < hints.size(); i++) {
            Group hintGroup = new Group(hintsContainer, SWT.NONE);
            hintGroup.setText("Hint " + (i + 1));
            hintGroup.setLayout(new GridLayout(1, false));
            hintGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            Text hintText = new Text(hintGroup, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
            hintText.setText(hints.get(i).getContent());
            GridData hintData = new GridData(SWT.FILL, SWT.TOP, true, false);
            hintData.heightHint = 60;
            hintText.setLayoutData(hintData);
        }

        // Show "Reveal next hint" button if more hints available
        if (revealedHints < hints.size()) {
            Button revealButton = new Button(hintsContainer, SWT.PUSH);
            int remaining = hints.size() - revealedHints;
            revealButton.setText("Reveal Next Hint (" + remaining + " remaining)");
            revealButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            revealButton.addListener(SWT.Selection, e -> {
                revealedHints++;
                updateHintsDisplay();
            });
        } else if (hints.size() > 0) {
            Label allRevealed = new Label(hintsContainer, SWT.NONE);
            allRevealed.setText("All hints revealed.");
        }

        hintsContainer.layout();
        hintsContainer.getParent().layout();
    }

    private void copyStarterCode() {
        if (currentExercise != null && currentExercise.getStarterCode() != null) {
            Clipboard clipboard = new Clipboard(Display.getDefault());
            TextTransfer textTransfer = TextTransfer.getInstance();
            clipboard.setContents(new Object[]{currentExercise.getStarterCode()}, new Transfer[]{textTransfer});
            clipboard.dispose();
        }
    }

    @Override
    public void setFocus() {
        if (descriptionBrowser != null) {
            descriptionBrowser.setFocus();
        }
    }
}
