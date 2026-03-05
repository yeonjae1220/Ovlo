package me.yeonjae.ovlo.domain.member.model;

public enum DegreeType {
    BACHELOR("B", 4),
    MASTER("M", 3),
    DOCTOR("D", 5);

    private final String prefix;
    private final int maxGrade;

    DegreeType(String prefix, int maxGrade) {
        this.prefix = prefix;
        this.maxGrade = maxGrade;
    }

    public String displayGrade(int grade) {
        return prefix + grade;
    }

    public int getMaxGrade() {
        return maxGrade;
    }
}
