package org.tree.model;

public class ApplicationInfo {
    private final String pid;
    private final String packageName;

    public ApplicationInfo(String pid, String packageName) {
        this.pid = pid;
        this.packageName = packageName;
    }

    public String pid() {
        return pid;
    }

    public String packageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "pid='" + pid + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
