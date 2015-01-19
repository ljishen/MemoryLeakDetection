package org.tree.model;

public class ClassInstanceCount {
    private final int count;
    private final int newCount;
    private final String className;

    public ClassInstanceCount(int count, int newCount, String className) {
        this.count = count;
        this.newCount = newCount;
        this.className = className;
    }

    public int getCount() {
        return count;
    }

    public int getNewCount() {
        return newCount;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "ClassInstanceCount{" +
                "count=" + count +
                ", newCount=" + newCount +
                ", className='" + className + '\'' +
                '}';
    }
}
