package project.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import project.model.User;
import project.util.JsonUtils;

public class UserService {

    private final String FILE = "data/users.json";
    private List<User> users = new ArrayList<>();

    public UserService() {
        loadUsers();
    }

    private void loadUsers() {
        File f = new File(FILE);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        
        if (!f.exists()) {
            users = new ArrayList<>();
            saveUsers();
            return;
        }
        users = JsonUtils.load(FILE, JsonUtils.listOf(User.class));
        if (users == null) users = new ArrayList<>();

        // --- VERİ ONARIMI (MIGRATION FIX) ---
        // Eski verilerde totalPoints yoktu, bu yüzden 0 geliyor.
        // Eğer kullanıcının parası var ama XP'si 0 ise, XP'yi paraya eşitle.
        boolean needSave = false;
        for (User u : users) {
            if (u.getTotalPoints() == 0 && u.getPoints() > 0) {
                u.setTotalPoints(u.getPoints());
                needSave = true;
            }
        }
        if (needSave) saveUsers();
        // ------------------------------------
    }

    public void saveUsers() {
        JsonUtils.save(FILE, users);
    }

    public void addUser(User u) {
        users.add(u);
        saveUsers();
    }

    public List<User> getAllUsers() { return users; }

    public User getUserById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    public User login(String name, String password) {
        return users.stream()
            .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(name) && u.getPassword().equals(password))
            .findFirst().orElse(null);
    }

    // --- PUAN YÖNETİMİ ---
    public void addPoints(String userId, int amount) {
        User u = getUserById(userId);
        if (u != null) {
            u.addPoints(amount); // User.java'daki mantık çalışır
            saveUsers();
        }
    }

    // --- RELATIONS ---
    public List<User> getChildrenOfParent(String parentId) {
        if (parentId == null) return new ArrayList<>();
        return users.stream()
                .filter(u -> "kid".equalsIgnoreCase(u.getRole()) && u.getParentIds() != null && u.getParentIds().contains(parentId))
                .collect(Collectors.toList());
    }

    public List<User> getChildrenOfParents(List<String> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) return new ArrayList<>();
        return users.stream()
                .filter(u -> "kid".equalsIgnoreCase(u.getRole()) && u.getParentIds() != null && u.getParentIds().stream().anyMatch(parentIds::contains))
                .collect(Collectors.toList());
    }

    public List<User> getKidsOfClass(String classId) {
        if (classId == null) return new ArrayList<>();
        return users.stream()
                .filter(u -> "kid".equalsIgnoreCase(u.getRole()) && classId.equals(u.getClassId()))
                .collect(Collectors.toList());
    }

    public List<User> getAllKids() {
        return users.stream()
                .filter(u -> "kid".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
    }

    public List<User> getKidsOfTeacher(String teacherId) {
        User t = getUserById(teacherId);
        if (t == null || t.getClassIds() == null) return new ArrayList<>();
        List<User> list = new ArrayList<>();
        for (String classId : t.getClassIds()) {
            list.addAll(getKidsOfClass(classId));
        }
        return list;
    }

    public boolean linkChildByUsername(User parent, String childUsername) {
        User child = users.stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(childUsername) && "kid".equals(u.getRole()))
                .findFirst().orElse(null);

        if (child != null) {
            if (parent.getChildrenIds() == null || !parent.getChildrenIds().contains(child.getId())) {
                parent.addChild(child.getId());
            }
            if (child.getParentIds() == null || !child.getParentIds().contains(parent.getId())) {
                child.addParent(parent.getId());
            }
            saveUsers();
            return true;
        }
        return false;
    }

    public User getKidByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username) && "kid".equals(u.getRole()))
                .findFirst()
                .orElse(null);
    }

    public User getUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    public void updateUser(User user) { saveUsers(); }
}