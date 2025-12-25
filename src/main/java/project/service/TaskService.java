package project.service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import project.model.Task;
import project.model.User;
import project.util.JsonUtils;

public class TaskService {

    private final String FILE = "data/tasks.json";
    private List<Task> tasks = new ArrayList<>();
    private final UserService userService;

    public TaskService(UserService u) {
        this.userService = u;
        loadTasks();
    }

    // ---------------- DOSYA İŞLEMLERİ ----------------
    private void loadTasks() {
        File f = new File(FILE);

        if (!f.exists()) {
            tasks = new ArrayList<>();
            saveTasks();
            return;
        }

        tasks = JsonUtils.load(FILE, JsonUtils.listOf(Task.class));
        if (tasks == null) tasks = new ArrayList<>();
    }

    private void saveTasks() {
        JsonUtils.save(FILE, tasks);
    }

    // ---------------- GÖREV EKLEME ----------------
    public void addTask(String kidId, String name, String desc, int points, LocalDate date, String freq, String assignedBy) {
        int id = (tasks.isEmpty()) ? 1 : tasks.get(tasks.size() - 1).getId() + 1;

        Task t = new Task(
                id, name, desc, date, points, "TODO", freq, assignedBy, kidId
        );

        tasks.add(t);
        saveTasks();
        System.out.println("Task created successfully!");
    }

    // ---------------- LİSTELEME ----------------
    public List<Task> getTasksOfKid(String kidId) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getKidId().equals(kidId))
                result.add(t);
        }
        return result;
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    // ---------------- İŞ MANTIĞI ----------------

    /**
     * ÇOCUK İÇİN: Görevi tamamladığını bildirir.
     * BURASI MODERN SWITCH (RULE SWITCH) OLDU
     */
    public void requestCompletion(int taskId) {
        for (Task t : tasks) {
            if (t.getId() == taskId) {
                
                // YENİ NESİL SWITCH (Rule Switch)
                switch (t.getStatus()) {
                    case "TODO" -> {
                        t.setStatus("PENDING");
                        saveTasks();
                        System.out.println("Task marked as done. Waiting for approval.");
                    }
                    case "PENDING" -> System.out.println("This task is already waiting for approval.");
                    default -> System.out.println("Task is already completed or invalid status.");
                }
                return;
            }
        }
        System.out.println("Task not found with ID: " + taskId);
    }

    /**
     * EBEVEYN/ÖĞRETMEN İÇİN: Görevi onaylar.
     */
    public void approveTask(int taskId) {
        for (Task t : tasks) {
            if (t.getId() == taskId) {
                if ("PENDING".equals(t.getStatus())) {
                    
                    t.setStatus("COMPLETED");
                    
                    User kid = userService.getUserById(t.getKidId());
                    if (kid != null) {
                        kid.addPoints(t.getPoints());
                        userService.saveUsers(); 
                        System.out.println("Approved! " + t.getPoints() + " points added to " + kid.getName());
                    } else {
                        System.out.println("Error: Kid user not found.");
                    }
                    
                    saveTasks();
                    
                } else {
                    System.out.println("Cannot approve. Task status is: " + t.getStatus());
                }
                return;
            }
        }
        System.out.println("Task not found with ID: " + taskId);
    }
}