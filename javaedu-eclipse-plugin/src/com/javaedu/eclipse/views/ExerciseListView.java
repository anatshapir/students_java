package com.javaedu.eclipse.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;

import com.javaedu.eclipse.Activator;
import com.javaedu.eclipse.model.Course;
import com.javaedu.eclipse.model.Exercise;
import com.javaedu.eclipse.services.AuthManager;
import com.javaedu.eclipse.services.ExerciseManager;

import java.util.List;

/**
 * View showing available exercises organized by course.
 */
public class ExerciseListView extends ViewPart {

    public static final String ID = "com.javaedu.eclipse.views.ExerciseListView";

    private TreeViewer treeViewer;
    private Text searchText;
    private Label statusLabel;

    @Override
    public void createPartControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // Search bar
        searchText = new Text(container, SWT.BORDER | SWT.SEARCH);
        searchText.setMessage("Search exercises...");
        searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        searchText.addModifyListener(e -> filterExercises());

        // Tree viewer for courses and exercises
        treeViewer = new TreeViewer(container, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        treeViewer.setContentProvider(new ExerciseTreeContentProvider());
        treeViewer.setLabelProvider(new ExerciseLabelProvider());

        // Double-click to open exercise
        treeViewer.addDoubleClickListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            Object selected = selection.getFirstElement();
            if (selected instanceof Exercise) {
                openExercise((Exercise) selected);
            }
        });

        // Status label
        statusLabel = new Label(container, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Refresh button
        Button refreshButton = new Button(container, SWT.PUSH);
        refreshButton.setText("Refresh");
        refreshButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        refreshButton.addListener(SWT.Selection, e -> refreshExercises());

        // Initial load
        if (AuthManager.getInstance().isLoggedIn()) {
            refreshExercises();
        } else {
            statusLabel.setText("Please login to view exercises");
        }
    }

    public void refreshExercises() {
        if (!AuthManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Please login first");
            return;
        }

        statusLabel.setText("Loading exercises...");

        // Run in background
        Display.getDefault().asyncExec(() -> {
            try {
                List<Course> courses = ExerciseManager.getInstance().getCourses();
                treeViewer.setInput(courses);
                statusLabel.setText("Found " + courses.size() + " course(s)");
            } catch (Exception e) {
                statusLabel.setText("Error loading exercises: " + e.getMessage());
            }
        });
    }

    private void filterExercises() {
        String filter = searchText.getText().toLowerCase();
        if (filter.isEmpty()) {
            treeViewer.resetFilters();
        } else {
            treeViewer.setFilters(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof Course) {
                        return true; // Always show courses
                    }
                    if (element instanceof Exercise) {
                        Exercise ex = (Exercise) element;
                        return ex.getTitle().toLowerCase().contains(filter) ||
                               ex.getDescription().toLowerCase().contains(filter);
                    }
                    return true;
                }
            });
        }
    }

    private void openExercise(Exercise exercise) {
        ExerciseDetailView detailView = (ExerciseDetailView) getSite()
                .getPage()
                .findView(ExerciseDetailView.ID);
        if (detailView != null) {
            detailView.showExercise(exercise);
        }
        ExerciseManager.getInstance().setCurrentExercise(exercise);
    }

    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    /**
     * Content provider for the exercise tree.
     */
    private static class ExerciseTreeContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof List) {
                return ((List<?>) inputElement).toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof Course) {
                return ((Course) parentElement).getExercises().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof Exercise) {
                return ((Exercise) element).getCourse();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof Course) {
                return !((Course) element).getExercises().isEmpty();
            }
            return false;
        }
    }

    /**
     * Label provider for the exercise tree.
     */
    private static class ExerciseLabelProvider extends LabelProvider {

        @Override
        public String getText(Object element) {
            if (element instanceof Course) {
                Course course = (Course) element;
                return course.getName() + " (" + course.getExercises().size() + " exercises)";
            }
            if (element instanceof Exercise) {
                Exercise exercise = (Exercise) element;
                String status = exercise.isCompleted() ? " ✓" : "";
                return exercise.getTitle() + " [" + exercise.getDifficulty() + "]" + status;
            }
            return super.getText(element);
        }

        @Override
        public Image getImage(Object element) {
            // Return appropriate icons based on type
            return null;
        }
    }
}
