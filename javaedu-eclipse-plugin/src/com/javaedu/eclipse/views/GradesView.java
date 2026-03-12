package com.javaedu.eclipse.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;

import com.javaedu.eclipse.model.Submission;
import com.javaedu.eclipse.services.ApiClient;
import com.javaedu.eclipse.services.AuthManager;

import java.util.List;

/**
 * View showing the student's grades and submission history.
 */
public class GradesView extends ViewPart {

    public static final String ID = "com.javaedu.eclipse.views.GradesView";

    private TableViewer tableViewer;
    private Label totalSubmissionsLabel;
    private Label summaryLabel;
    private Text feedbackText;

    @Override
    public void createPartControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // Summary section
        Group summaryGroup = new Group(container, SWT.NONE);
        summaryGroup.setText("Summary");
        summaryGroup.setLayout(new GridLayout(4, false));
        summaryGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        new Label(summaryGroup, SWT.NONE).setText("Total Submissions:");
        totalSubmissionsLabel = new Label(summaryGroup, SWT.NONE);
        totalSubmissionsLabel.setText("0");

        new Label(summaryGroup, SWT.NONE).setText("Average Grade:");
        summaryLabel = new Label(summaryGroup, SWT.NONE);
        summaryLabel.setText("N/A");

        // Grades table
        tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Create columns
        createTableColumns();

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setLabelProvider(new GradesLabelProvider());

        // Selection listener to show feedback
        tableViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (!selection.isEmpty()) {
                Submission submission = (Submission) selection.getFirstElement();
                showFeedback(submission);
            }
        });

        // Feedback section
        Group feedbackGroup = new Group(container, SWT.NONE);
        feedbackGroup.setText("Feedback");
        feedbackGroup.setLayout(new GridLayout(1, false));
        feedbackGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        feedbackText = new Text(feedbackGroup, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        GridData feedbackData = new GridData(SWT.FILL, SWT.FILL, true, true);
        feedbackData.heightHint = 100;
        feedbackText.setLayoutData(feedbackData);

        // Refresh button
        Button refreshButton = new Button(container, SWT.PUSH);
        refreshButton.setText("Refresh Grades");
        refreshButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        refreshButton.addListener(SWT.Selection, e -> refreshGrades());

        // Initial load
        if (AuthManager.getInstance().isLoggedIn()) {
            refreshGrades();
        }
    }

    private void createTableColumns() {
        Table table = tableViewer.getTable();

        TableColumn exerciseColumn = new TableColumn(table, SWT.LEFT);
        exerciseColumn.setText("Exercise");
        exerciseColumn.setWidth(200);

        TableColumn dateColumn = new TableColumn(table, SWT.LEFT);
        dateColumn.setText("Submitted");
        dateColumn.setWidth(150);

        TableColumn statusColumn = new TableColumn(table, SWT.CENTER);
        statusColumn.setText("Status");
        statusColumn.setWidth(100);

        TableColumn scoreColumn = new TableColumn(table, SWT.CENTER);
        scoreColumn.setText("Score");
        scoreColumn.setWidth(80);

        TableColumn percentColumn = new TableColumn(table, SWT.CENTER);
        percentColumn.setText("Grade");
        percentColumn.setWidth(80);
    }

    public void refreshGrades() {
        if (!AuthManager.getInstance().isLoggedIn()) {
            return;
        }

        Display.getDefault().asyncExec(() -> {
            try {
                List<Submission> submissions = ApiClient.getInstance().getMySubmissions();
                tableViewer.setInput(submissions);

                // Update summary
                totalSubmissionsLabel.setText(String.valueOf(submissions.size()));
                if (!submissions.isEmpty()) {
                    double avgGrade = submissions.stream()
                            .filter(s -> s.getGrade() != null)
                            .mapToDouble(s -> s.getGrade().getPercentage())
                            .average()
                            .orElse(0);
                    summaryLabel.setText(String.format("%.1f%%", avgGrade));
                }
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    private void showFeedback(Submission submission) {
        if (submission.getGrade() != null && submission.getGrade().getFeedback() != null) {
            feedbackText.setText(submission.getGrade().getFeedback());
        } else {
            feedbackText.setText("No feedback available.");
        }
    }

    @Override
    public void setFocus() {
        tableViewer.getControl().setFocus();
    }

    /**
     * Label provider for the grades table.
     */
    private static class GradesLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof Submission)) {
                return "";
            }

            Submission submission = (Submission) element;

            switch (columnIndex) {
                case 0: // Exercise
                    return submission.getExerciseTitle();
                case 1: // Date
                    return submission.getSubmittedAt();
                case 2: // Status
                    return submission.getStatus();
                case 3: // Score
                    if (submission.getGrade() != null) {
                        return submission.getGrade().getScore() + "/" + submission.getGrade().getMaxScore();
                    }
                    return "Pending";
                case 4: // Percentage
                    if (submission.getGrade() != null) {
                        return String.format("%.1f%%", submission.getGrade().getPercentage());
                    }
                    return "N/A";
                default:
                    return "";
            }
        }

        @Override
        public org.eclipse.swt.graphics.Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }
}
