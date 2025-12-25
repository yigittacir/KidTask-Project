package project.model;

public class Wish {

    private int id;
    private String name;
    private String description;

    private int requiredPoints;
    private int requiredLevel;

    private boolean approved;
    private boolean purchased;

    private String kidId;

    public void setRequiredPoints(int p) { this.requiredPoints = p; }
    public void setRequiredLevel(int l) { this.requiredLevel = l; }

    public Wish() {}

    // *** BU CONSTRUCTOR ZORUNLU ***
    public Wish(int id, String name, String description,
                int requiredPoints, int requiredLevel,
                boolean approved, boolean purchased,
                String kidId) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredPoints = requiredPoints;
        this.requiredLevel = requiredLevel;
        this.approved = approved;
        this.purchased = purchased;
        this.kidId = kidId;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRequiredPoints() { return requiredPoints; }
    public int getRequiredLevel() { return requiredLevel; }
    public boolean isApproved() { return approved; }
    public boolean isPurchased() { return purchased; }
    public String getKidId() { return kidId; }

    // --- SETTERS ---
    public void setApproved(boolean val) { this.approved = val; }
    public void setPurchased(boolean val) { this.purchased = val; }

    @Override
    public String toString() {
        return "Wish #" + id + " | " + name +
                " | Points: " + requiredPoints +
                " | Level: " + requiredLevel +
                " | Approved: " + approved +
                " | Purchased: " + purchased;
    }
}
