package com.kids.configuration;

import com.kids.entities.SchoolYear;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class AppContext {

    // نسخة وحيدة من الكلاس يتم مشاركتها في التطبيق بالكامل
    private static final AppContext instance = new AppContext();

    // استخدام ObjectProperty يتيح للواجهات مراقبة التغييرات فوراً (Data Binding)
    private final ObjectProperty<SchoolYear> currentSchoolYear = new SimpleObjectProperty<>();

    // منع إنشاء نسخة جديدة من الكلاس من الخارج
    private AppContext() {}

    public static AppContext getInstance() {
        return instance;
    }

    public SchoolYear getCurrentSchoolYear() {
        return currentSchoolYear.get();
    }

    public void setCurrentSchoolYear(SchoolYear schoolYear) {
        this.currentSchoolYear.set(schoolYear);
    }

    public ObjectProperty<SchoolYear> currentSchoolYearProperty() {
        return currentSchoolYear;
    }
}