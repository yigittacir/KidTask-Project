package project.model;

import java.time.LocalDate;

public class Task {

    private int id;
    private String taskName;
    private String description;
    private LocalDate dueDate;
    private int points;
    
    // --- ÖNEMLİ: boolean completed SİLİNDİ, status GELDİ ---
    private String status; // "todo", "PENDING", "COMPLETED"

    private String frequency;
    private String assignedBy;
    private String kidId;

    public Task() {}

    public Task(int id, String name, String desc, LocalDate date,
                int points, String status, String freq,
                String assignedBy, String kidId) {

        this.id = id;
        this.taskName = name;
        this.description = desc;
        this.dueDate = date;
        this.points = points;
        this.status = status;
        this.frequency = freq;
        this.assignedBy = assignedBy;
        this.kidId = kidId;
    }

    public int getId() { return id; }
    public String getTaskName() { return taskName; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public int getPoints() { return points; }
    public String getStatus() { return status; } // Getter değişti
    public String getFrequency() { return frequency; }
    public String getAssignedBy() { return assignedBy; }
    public String getKidId() { return kidId; }

    public void setStatus(String status) { this.status = status; }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        // Konsolda daha düzenli görünmesi için tarihi de ekledik
        return String.format("Task #%d | %s | Due: %s | Pts: %d | Status: %s", 
                             id, taskName, dueDate, points, status);
    }
}