package com.kma.ojcore.enums;

public enum RuleType {
    ACM, // ACM (Association for Computing Machinery) - Thí sinh được phép nộp nhiều lần, chỉ tính số lần nộp sai trước khi nộp đúng. Điểm số dựa trên số lượng test case đúng và thời gian nộp.
    OI // OI (Olympiad in Informatics) - Thí sinh chỉ được nộp một lần duy nhất cho mỗi bài tập. Điểm số dựa trên số lượng test case đúng, không tính thời gian nộp.
}
