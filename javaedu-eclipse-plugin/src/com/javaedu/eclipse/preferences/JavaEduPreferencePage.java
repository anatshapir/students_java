package com.javaedu.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.javaedu.eclipse.Activator;
import com.javaedu.eclipse.services.ApiClient;

/**
 * Preference page for JavaEdu settings.
 */
public class JavaEduPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public JavaEduPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("JavaEdu Plugin Settings");
    }

    @Override
    public void createFieldEditors() {
        addField(new StringFieldEditor(
                PreferenceConstants.SERVER_URL,
                "Server URL:",
                getFieldEditorParent()));
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        if (result) {
            String serverUrl = getPreferenceStore().getString(PreferenceConstants.SERVER_URL);
            if (serverUrl != null && !serverUrl.isEmpty()) {
                ApiClient.getInstance().setBaseUrl(serverUrl);
            }
        }
        return result;
    }

    @Override
    public void init(IWorkbench workbench) {
        // Nothing to initialize
    }
}
