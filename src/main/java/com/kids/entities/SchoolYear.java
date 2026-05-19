package com.kids.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "school_years")
@Getter
@Setter
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // تأكيد قراءة المعرف
    private Long id;

    // حرج جداً: الـ name هنا يجب أن يطابق اسم العمود في الـ SQL حرفياً وبحروف صغيرة
    @Column(name = "year_name", nullable = false)
    private String yearName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // إلزامية وجود الكونسلوكتور الفارغ يدوياً بدون الاعتماد على لومبوك لقطع الشك باليقين
    public SchoolYear() {
    }

    public SchoolYear(Long id, String yearName, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.yearName = yearName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return this.yearName == null ? "" : this.yearName;
    }
}