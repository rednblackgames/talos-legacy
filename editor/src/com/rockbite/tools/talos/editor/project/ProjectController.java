package com.rockbite.tools.talos.editor.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import com.rockbite.tools.talos.TalosMain;
import com.rockbite.tools.talos.editor.widgets.ui.FileTab;

import java.io.File;
import java.util.Comparator;

public class ProjectController {

    private String currentProjectPath = null;
    private String projectFileName = null;
    public FileTab currentTab;
    private ObjectMap<String, String> fileCache = new ObjectMap<>();
    private ObjectMap<String, String> pathCache = new ObjectMap<>();
    private ObjectMap<String, String> exporthPathCache = new ObjectMap<>();
    private boolean loading = false;

    IProject currentProject;

    public static TalosProject TLS = new TalosProject();
    private boolean lastDirTracking = true;

    public ProjectController() {
        currentProject = TLS;
    }

    public void loadProject (FileHandle projectFileHandle) {
        if (projectFileHandle.exists()) {
            FileTab prevTab = currentTab;
            boolean removingUnworthy = false;

            if(currentTab != null) {
                if(currentTab.getProjectType() == currentProject && currentTab.isUnworthy()) {
                    removingUnworthy = true;
                    clearCache(currentTab.getFileName());
                } else {
                    IProject tmp = currentProject;
                    currentProject = currentTab.getProjectType();
                    saveProjectToCache(projectFileName);
                    currentProject = tmp;
                }
            }
            currentProjectPath = projectFileHandle.path();
            projectFileName = projectFileHandle.name();
            loading = true;
            currentTab = new FileTab(projectFileName, currentProject); // trackers need to know what current tab is
            currentProject.loadProject(projectFileHandle.readString());
            reportProjectFileInterraction(projectFileHandle);
            loading = false;

            if(lastDirTracking) {
                TalosMain.Instance().Prefs().putString("lastOpen" + currentProject.getExtension(), projectFileHandle.parent().path());
                TalosMain.Instance().Prefs().flush();
            }

            TalosMain.Instance().UIStage().tabbedPane.add(currentTab);

            final Array<String> savedResourcePaths = currentProject.getSavedResourcePaths();
            TalosMain.Instance().FileTracker().addSavedResourcePathsFor(currentTab, savedResourcePaths);

            if(removingUnworthy) {
                safeRemoveTab(prevTab);
            }
        } else {
            //error handle
        }
    }

    private void saveProjectToCache(String projectFileName) {
        fileCache.put(projectFileName, currentProject.getProjectString());
        pathCache.put(projectFileName, currentProjectPath);
    }

    private void getProjectFromCache(String projectFileName) {
        loading = true;
        currentProjectPath = pathCache.get(projectFileName);
        currentProject.loadProject(fileCache.get(projectFileName));
        loading = false;
    }

    public void saveProject (FileHandle destination) {
        String data = currentProject.getProjectString();
        destination.writeString(data, false);

        reportProjectFileInterraction(destination);

        TalosMain.Instance().Prefs().putString("lastSave"+currentProject.getExtension(), destination.parent().path());
        TalosMain.Instance().Prefs().flush();

        currentTab.setDirty(false);
        currentTab.setWorthy();
        currentProjectPath = destination.path();
        projectFileName = destination.name();

        if(!currentTab.getFileName().equals(projectFileName)) {
            clearCache(currentTab.getFileName());
            currentTab.setFileName(projectFileName);
            TalosMain.Instance().UIStage().tabbedPane.updateTabTitle(currentTab);
            fileCache.put(projectFileName, data);
        }
    }

    public void saveProject() {
        if(isBoundToFile()) {
            FileHandle handle = Gdx.files.absolute(currentProjectPath);
            saveProject(handle);
        }
    }

    public void newProject (IProject project) {
        FileTab prevTab = currentTab;

        boolean removingUnworthy = false;

        if(currentTab != null) {
            if(currentTab.getProjectType() == project && currentTab.isUnworthy()) {
                removingUnworthy = true;
                clearCache(currentTab.getFileName());
            }  else {
                saveProjectToCache(projectFileName);
            }
        }

        String newName = getNewFilename(project);
        FileTab tab = new FileTab(newName, project);
        tab.setUnworthy(); // all new projects are unworthy, and will only become worthy when worked on
        TalosMain.Instance().UIStage().tabbedPane.add(tab);

        TalosMain.Instance().FileTracker().addTab(tab);

        currentProject.resetToNew();
        currentProjectPath = null;

        if(removingUnworthy) {
            safeRemoveTab(prevTab);
        }
    }

    /**
     * removes tab without listener crap
     */
    public void safeRemoveTab(FileTab tab) {
        FileTab tmp = currentTab;
        TalosMain.Instance().UIStage().tabbedPane.remove(tab);
        currentTab = tmp;
    }

    public String getNewFilename(IProject project) {
        int index = 1;
        String name = project.getProjectNameTemplate() + index + project.getExtension();
        while (fileCache.containsKey(name)) {
            index++;
            name = project.getProjectNameTemplate() + index + project.getExtension();
        }

        return name;
    }

    public boolean isBoundToFile() {
        return currentProjectPath != null;
    }

    public void unbindFromFile() {
        currentProjectPath = null;
    }

    public String getCurrentProjectPath () {
        return currentProjectPath;
    }


    public void setDirty() {
        if(!loading) {
            currentTab.setDirty(true);
            currentTab.setWorthy();
        }
    }

    public void loadFromTab(FileTab tab) {
        String fileName = tab.getFileName();

        if(currentTab != null && currentTab != tab) {
            saveProjectToCache(projectFileName);
        }
        if(fileCache.containsKey(fileName)) {
            currentProject = tab.getProjectType();
            currentTab =  tab;
            getProjectFromCache(fileName);
        }

        projectFileName = fileName;
        currentTab = tab;
        currentProject = currentTab.getProjectType();
        if(tab.getProjectType() == TLS) {
            TalosMain.Instance().UIStage().swapToTalosContent();
        } else {
            currentProject.initUIContent();
        }
    }

    public void removeTab(FileTab tab) {
        String fileName = tab.getFileName();
        clearCache(fileName);
        if(tab == currentTab) {
            currentTab = null;
        }
    }

    public void clearCache(String fileName) {
        pathCache.remove(fileName);
        fileCache.remove(fileName);
    }

    public void setProject(IProject project) {
        currentProject = project;
        if(project.equals(TLS)) {
            TalosMain.Instance().UIStage().swapToTalosContent();
        }
    }

    public IProject getProject() {
        return currentProject;
    }

    public FileHandle findFile(String path) {
        return findFile(Gdx.files.absolute(path));
    }

    public FileHandle findFile(FileHandle initialFile) {
        String fileName = initialFile.name();

        // local is priority, then the path, then the default lookup
        // do we currently have project loaded?
        if(currentProjectPath != null) {
            // we can look for local file then
            FileHandle currentProjectHandle = Gdx.files.absolute(currentProjectPath);
            if(currentProjectHandle.exists()) {
                String localPath = currentProjectHandle.parent().path() + File.separator + fileName;
                FileHandle localTry = Gdx.files.absolute(localPath);
                if(localTry.exists()) {
                    return localTry;
                }
            }
        }

        //Maybe the absolute path was a better ideas
        if(initialFile.exists()) return initialFile;

        //oh crap it's nowhere to be found, default path to the rescue!
        FileHandle lastHopeHandle = currentProject.findFileInDefaultPaths(fileName);
        if(lastHopeHandle != null && lastHopeHandle.exists()) {
            return lastHopeHandle;
        }

        // well we did all we could. seppuku is imminent
        return null;
    }

    public void exportProject(FileHandle fileHandle) {
        exporthPathCache.put(projectFileName, fileHandle.path());

        String data = currentProject.exportProject();
        fileHandle.writeString(data, false);

        TalosMain.Instance().Prefs().putString("lastExport"+currentProject.getExtension(), fileHandle.parent().path());
        TalosMain.Instance().Prefs().flush();
    }

    public String getCurrentExportNameSuggestion() {
        if(currentTab != null) {
            String projectName = currentTab.getFileName();
            String exportExt = currentProject.getExportExtension();
            return projectName.substring(0, projectName.lastIndexOf(".")) + exportExt;
        }
        return "";
    }

    public String getLastDir(String action, IProject projectType) {
        String path = TalosMain.Instance().Prefs().getString("last" + action + projectType.getExtension());
        FileHandle handle = Gdx.files.absolute(path);
        if(handle.exists()) {
            return handle.path();
        }

        return "";
    }

    public String getExportPath() {
        return exporthPathCache.get(projectFileName);
    }

    public void lastDirTrackingDisable() {
        lastDirTracking = false;
    }

    public void lastDirTrackingEnable() {
        lastDirTracking = true;
    }

    public static class RecentsEntry {
        String path;
        long time;

        public RecentsEntry() {

        }

        public RecentsEntry(String path, long time) {
            this.path = path;
            this.time = time;
        }

        @Override
        public boolean equals(Object obj) {
            return path.equals(((RecentsEntry)obj).path);
        }
    }

    Comparator<RecentsEntry> recentsEntryComparator = new Comparator<RecentsEntry>() {
        @Override
        public int compare(RecentsEntry o1, RecentsEntry o2) {
            return (int) (o2.time - o1.time);
        }
    };

    public void reportProjectFileInterraction(FileHandle handle) {
        Preferences prefs = TalosMain.Instance().Prefs();
        String data = prefs.getString("recents");
        Array<RecentsEntry> list = new Array<>();
        //read
        Json json = new Json();
        if(data != null && !data.isEmpty()) {
            list = json.fromJson(list.getClass(), data);
        }
        RecentsEntry newEntry = new RecentsEntry(handle.path(), TimeUtils.millis());
        list.removeValue(newEntry, false);
        list.add(newEntry);
        //sort
        list.sort(recentsEntryComparator);
        //write
        String result = json.toJson(list);
        prefs.putString("recents", result);
        prefs.flush();
        updateRecentsList();
    }

    public Array<String> updateRecentsList() {
        Preferences prefs = TalosMain.Instance().Prefs();
        String data = prefs.getString("recents");
        Array<String> list = new Array<>();
        //read
        Json json = new Json();
        if(data != null && !data.isEmpty()) {
            Array<RecentsEntry> rList = new Array<>();
            rList = json.fromJson(rList.getClass(), data);
            for(RecentsEntry entry: rList) {
                list.add(entry.path);
            }
        }

        TalosMain.Instance().UIStage().Menu().updateRecentsList(list);

        return list;
    }
}
