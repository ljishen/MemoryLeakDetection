package org.tree.model;

public class ApplicationInfo {
    private final String deviceSerialNumber;
    private final String deviceQualifiers;
    private String packageName;
    private String pid;

    public ApplicationInfo(
            String deviceSerialNumber,
            String deviceQualifiers) {
        this.deviceSerialNumber = deviceSerialNumber;
        this.deviceQualifiers = deviceQualifiers;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String deviceSerialNumber() {
        return deviceSerialNumber;
    }

    public String deviceQualifiers() {
        return deviceQualifiers;
    }

    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "deviceSerialNumber='" + deviceSerialNumber + '\'' +
                ", deviceQualifiers='" + deviceQualifiers + '\'' +
                ", packageName='" + packageName + '\'' +
                ", pid='" + pid + '\'' +
                '}';
    }
}
