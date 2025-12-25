package project.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import project.model.Wish;
import project.util.JsonUtils;

public class WishService {

    private final String FILE = "data/wishes.json";
    private List<Wish> wishes = new ArrayList<>();
    private final UserService userService;

    public WishService(UserService u) {
        this.userService = u;
        loadWishes();
    }

    private void loadWishes() {
        File f = new File(FILE);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        
        if (!f.exists()) {
            wishes = new ArrayList<>();
            saveWishes();
            return;
        }
        wishes = JsonUtils.load(FILE, JsonUtils.listOf(Wish.class));
        if (wishes == null) wishes = new ArrayList<>();
    }

    private void saveWishes() {
        JsonUtils.save(FILE, wishes);
    }

    public void requestWish(String kidId, String name, int cost) {
        int id = (wishes.isEmpty()) ? 1 : wishes.get(wishes.size() - 1).getId() + 1;
        // Varsayılan olarak Level 1 istenir
        Wish w = new Wish(id, name, "...", cost, 1, false, false, kidId);
        wishes.add(w);
        saveWishes();
    }

    public List<Wish> getWishesOfKid(String kidId) {
        List<Wish> result = new ArrayList<>();
        for (Wish w : wishes) {
            if (w.getKidId().equals(kidId)) result.add(w);
        }
        return result;
    }

    // --- SATIN ALMA (LEVEL KONTROLÜ EKLENDİ) ---
    public boolean purchaseWish(String kidId, int wishId) {
        for (Wish w : wishes) {
            if (w.getId() == wishId && w.getKidId().equals(kidId) && w.isApproved() && !w.isPurchased()) {
                
                var kid = userService.getUserById(kidId);

                // KONTROL 1: Bakiye Yetiyor mu?
                if (kid != null && kid.getPoints() >= w.getRequiredPoints()) {
                    
                    // KONTROL 2: Level Yetiyor mu? (YENİ)
                    if (kid.getLevel() >= w.getRequiredLevel()) {
                        // İşlemi yap
                        userService.addPoints(kidId, -w.getRequiredPoints());
                        w.setPurchased(true);
                        saveWishes();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Parent onaylarken Level bilgisini de kaydediyoruz
    public void approveWish(int wishId, int points, int level) {
        for (Wish w : wishes) {
            if (w.getId() == wishId) {
                w.setRequiredPoints(points);
                w.setRequiredLevel(level); // Level'i kaydet
                w.setApproved(true);
                saveWishes();
                return;
            }
        }
    }
}