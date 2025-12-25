package project.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {

    private String id;
    private String name;
    private String username;
    private String surname;
    private int age;
    private String role; // kid, parent, teacher
    private String password;

    private int points = 0;      // Harcanabilir Puan (Cüzdan/Cash)
    private int totalPoints = 0; // Toplam Kazanılan XP (Liderlik ve Level için)

    private String classId;           
    private List<String> childrenIds; 
    private List<String> classIds;    
    private List<String> parentIds;   
    private List<String> teacherIds;  

    public User() {}

    public User(String name, String surname, String role, String password) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.username = name; // default username = name, can be overridden
        this.surname = surname;
        this.role = role;
        this.password = password;
        this.childrenIds = new ArrayList<>();
        this.classIds = new ArrayList<>();
        this.parentIds = new ArrayList<>();
        this.teacherIds = new ArrayList<>();
        this.points = 0;
        this.totalPoints = 0;
    }

    // --- FACTORY METHODS ---
    public static User createKid(String name, String surname, int age, String classId, String password) {
        User u = new User(name, surname, "kid", password);
        u.age = age;
        u.classId = classId;
        return u;
    }

    public static User createParent(String name, String surname, String password) {
        return new User(name, surname, "parent", password);
    }

    public static User createTeacher(String name, String surname, String password) {
        return new User(name, surname, "teacher", password);
    }

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public int getAge() { return age; }
    public String getRole() { return role; }
    public String getPassword() { return password; }
    
    // CÜZDAN (Cash)
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; } 
    
    // XP (Skor)
    public int getTotalPoints() { return totalPoints; } 
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    // --- LEVEL (OTOMATİK HESAPLAMA) ---
    // @JsonIgnore kaldırıldı. Yerine aşağıya boş setLevel eklendi.
    public int getLevel() {
        if (totalPoints < 0) return 1;
        // Her 150 puanda 1 level (0-149: Lvl 1, 150-299: Lvl 2...)
        return (totalPoints / 150) + 1;
    }

    // Bu metod JSON okurken hata almamak için var (İçi boş)
    public void setLevel(int level) { 
        // Level otomatik hesaplandığı için dışarıdan set edilmesine gerek yok
    }
    
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }
    
    public List<String> getClassIds() { return classIds; }
    public void setClassIds(List<String> classIds) { this.classIds = classIds; }
    
    public List<String> getParentIds() { return parentIds; }
    public void setParentIds(List<String> parentIds) { this.parentIds = parentIds; }

    public List<String> getTeacherIds() { return teacherIds; }
    public void setTeacherIds(List<String> teacherIds) { this.teacherIds = teacherIds; }

    // --- RELATION METHODS ---
    public void addChild(String childId) {
        if (childrenIds == null) childrenIds = new ArrayList<>();
        childrenIds.add(childId);
    }

    public void addParent(String parentId) {
        if (parentIds == null) parentIds = new ArrayList<>();
        if (!parentIds.contains(parentId)) parentIds.add(parentId);
    }

    public void addTeacher(String teacherId) {
        if (teacherIds == null) teacherIds = new ArrayList<>();
        if (!teacherIds.contains(teacherId)) teacherIds.add(teacherId);
    }

    public void addClass(String classId) {
        if (classIds == null) classIds = new ArrayList<>();
        classIds.add(classId);
    }

    // --- PUAN MANTIĞI ---
    public void addPoints(int p) {
        if (p > 0) {
            // KAZANÇ: Hem cüzdan artar hem XP artar
            this.points += p;
            this.totalPoints += p;
        } else {
            // HARCAMA: Sadece cüzdan azalır (XP korunur)
            this.points += p; 
        }
    }

    @Override
    public String toString() {
        return name + " (Lvl " + getLevel() + ")";
    }
}