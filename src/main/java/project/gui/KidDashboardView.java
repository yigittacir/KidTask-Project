package project.gui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import project.model.Task;
import project.model.User;
import project.model.Wish;
import project.service.TaskService;
import project.service.UserService;
import project.service.WishService;

public class KidDashboardView {

    private final User currentKid;
    private final UserService userService = new UserService();
    private final TaskService taskService;
    private final WishService wishService;

    // Pencere sahipliği için ana sahne referansı
    private Stage dashboardStage;

    // UI Elemanları
    private TableView<Task> taskTable;
    private TableView<Wish> wishTable;
    private TilePane leaderboardPane; 
    
    private Label pointsLabel; // Cash
    private Label levelLabel;  // Level
    private Label leaderboardTitle; 

    // Filtreler
    private String currentTaskFilter = "TODO"; 
    private String currentWishFilter = "APPROVED"; 
    private String currentLeaderboardFilter = "SIBLINGS"; 

    public KidDashboardView(User kid) {
        this.currentKid = kid;
        this.taskService = new TaskService(userService);
        this.wishService = new WishService(userService);
    }

    public void show() {
        dashboardStage = new Stage(); // Ana sahneyi değişkene atıyoruz
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // HEADER
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.getStyleClass().add("top-header");

        Label appTitle = new Label("KidTask");
        appTitle.getStyleClass().add("app-title");

        HBox userBadgeBox = new HBox(15);
        userBadgeBox.setAlignment(Pos.CENTER);
        userBadgeBox.getStyleClass().add("user-tag");
        
        Label nameLbl = new Label("👋 Hoş Geldin, " + formatText(currentKid.getName()));
        nameLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: #7f8c8d;");
        
        levelLabel = new Label("Lvl " + currentKid.getLevel());
        levelLabel.setStyle("-fx-font-weight:900; -fx-text-fill: #8e44ad;"); 

        pointsLabel = new Label(currentKid.getPoints() + " 💰");
        pointsLabel.setStyle("-fx-font-weight:900; -fx-text-fill: #27ae60; -fx-font-size: 16px;"); 

        userBadgeBox.getChildren().addAll(nameLbl, new Separator(javafx.geometry.Orientation.VERTICAL), levelLabel, new Separator(javafx.geometry.Orientation.VERTICAL), pointsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("⟳ Yenile");
        refreshBtn.getStyleClass().add("button-refresh");
        refreshBtn.setOnAction(e -> refreshAllData());

        Button logoutBtn = new Button("Çıkış Yap");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setOnAction(e -> {
            dashboardStage.close();
            new LoginView().show(new Stage());
        });

        topBar.getChildren().addAll(appTitle, userBadgeBox, spacer, refreshBtn, logoutBtn);
        root.setTop(topBar);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab taskTab = new Tab("Görevlerim 📝", createTaskTabContent());
        Tab wishTab = new Tab("Dilekler 🎁", createWishTabContent());
        Tab leaderTab = new Tab("Liderlik Tablosu 🏆", createLeaderboardTabContent());
        tabPane.getTabs().addAll(taskTab, wishTab, leaderTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1100, 750);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) { }
        
        dashboardStage.setTitle("KidTask - Öğrenci Paneli");
        dashboardStage.setScene(scene);
        dashboardStage.show();
        
        refreshAllData();
    }

    // --- TAB 1: GÖREVLER ---
    private VBox createTaskTabContent() {
        VBox vbox = new VBox(20); vbox.setPadding(new Insets(20));
        HBox filterBox = new HBox(20); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblStatus = new Label("GÖREVLER:"); lblStatus.getStyleClass().add("filter-label");
        ToggleButton btnTodo = new ToggleButton("Yapılacaklar");
        ToggleButton btnPending = new ToggleButton("Onay Bekleyenler ⏳");
        ToggleButton btnCompleted = new ToggleButton("Tamamlananlar ✅");
        ToggleGroup group = new ToggleGroup();
        btnTodo.setToggleGroup(group); btnPending.setToggleGroup(group); btnCompleted.setToggleGroup(group);
        btnTodo.setSelected(true);
        btnTodo.getStyleClass().add("filter-toggle"); btnPending.getStyleClass().add("filter-toggle"); btnCompleted.getStyleClass().add("filter-toggle");
        
        btnTodo.setOnAction(e -> { if(btnTodo.isSelected()) { currentTaskFilter = "TODO"; refreshAllData(); }});
        btnPending.setOnAction(e -> { if(btnPending.isSelected()) { currentTaskFilter = "PENDING"; refreshAllData(); }});
        btnCompleted.setOnAction(e -> { if(btnCompleted.isSelected()) { currentTaskFilter = "COMPLETED"; refreshAllData(); }});
        
        filterBox.getChildren().addAll(lblStatus, btnTodo, btnPending, btnCompleted);
        
        taskTable = new TableView<>();
        taskTable.setPlaceholder(new Label("Harika! Yapılacak işin kalmadı 🎉")); 
        
        // 1. Görev Adı
        TableColumn<Task, String> nameCol = new TableColumn<>("Görev");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(formatText(data.getValue().getTaskName())));
        
        // 2. Açıklama
        TableColumn<Task, String> descCol = new TableColumn<>("Açıklama");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(formatText(data.getValue().getDescription())));
        
        // 3. YENİ: Son Tarih
        TableColumn<Task, String> dateCol = new TableColumn<>("Son Tarih 📅");
        dateCol.setPrefWidth(120);
        dateCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #e67e22; -fx-font-weight:bold;");
        dateCol.setCellValueFactory(data -> {
            LocalDate d = data.getValue().getDueDate();
            if (d == null) return new SimpleStringProperty("-");
            // Tarihi gün.ay.yıl formatına çeviriyoruz
            return new SimpleStringProperty(d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        });

        // 4. Ödül
        TableColumn<Task, Integer> pointCol = new TableColumn<>("Ödül");
        pointCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPoints()));
        pointCol.setStyle("-fx-alignment: CENTER;");
        
        // 5. İşlem Butonu
        TableColumn<Task, Void> actionCol = new TableColumn<>("İşlem");
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Tamamla ✅");
            {
                btn.getStyleClass().add("button-secondary"); btn.setStyle("-fx-min-width: 110px;");
                btn.setOnAction(e -> {
                    Task t = getTableView().getItems().get(getIndex());
                    taskService.requestCompletion(t.getId());
                    showCustomDialog("SUCCESS", "Harika İş! 🎉", "Görevi tamamladın! Ebeveynin onaylayınca puan hesabında olacak.");
                    refreshAllData();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); } else {
                    Task t = getTableView().getItems().get(getIndex());
                    if ("TODO".equals(t.getStatus())) setGraphic(btn);
                    else if ("PENDING".equals(t.getStatus())) { Label lbl = new Label("Onay Bekleniyor..."); lbl.setStyle("-fx-text-fill: #f39c12; -fx-font-weight:bold;"); setGraphic(lbl); }
                    else if ("COMPLETED".equals(t.getStatus())) { Label lbl = new Label("Tamamlandı +"+t.getPoints()); lbl.setStyle("-fx-text-fill: #27ae60; -fx-font-weight:bold;"); setGraphic(lbl); }
                    else setGraphic(null);
                }
            }
        });
        
        // Sütunları ekle (Tarih eklendi)
        taskTable.getColumns().addAll(nameCol, descCol, dateCol, pointCol, actionCol);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(taskTable, Priority.ALWAYS);
        vbox.getChildren().addAll(filterBox, taskTable);
        return vbox;
    }

    // --- TAB 2: DİLEKLER ---
    private VBox createWishTabContent() {
        VBox vbox = new VBox(20); vbox.setPadding(new Insets(20));
        HBox filterBox = new HBox(15); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblFilter = new Label("MARKET & ÇANTAM:"); lblFilter.getStyleClass().add("filter-label");
        
        ToggleButton btnApproved = new ToggleButton("Market (Onaylananlar) 🛒");
        ToggleButton btnPurchased = new ToggleButton("Çantam (Alınanlar) 🎒");
        ToggleButton btnPending = new ToggleButton("Bekleyenler ⏳");
        ToggleGroup wGroup = new ToggleGroup();
        btnApproved.setToggleGroup(wGroup); btnPurchased.setToggleGroup(wGroup); btnPending.setToggleGroup(wGroup); 
        btnApproved.setSelected(true);
        btnApproved.getStyleClass().add("filter-toggle"); btnPurchased.getStyleClass().add("filter-toggle"); btnPending.getStyleClass().add("filter-toggle");
        
        btnApproved.setOnAction(e -> { if(btnApproved.isSelected()) { currentWishFilter = "APPROVED"; wishTable.setPlaceholder(new Label("Henüz onaylanmış bir dileğin yok.")); refreshAllData(); }});
        btnPurchased.setOnAction(e -> { if(btnPurchased.isSelected()) { currentWishFilter = "PURCHASED"; wishTable.setPlaceholder(new Label("Henüz bir şey satın almadın.")); refreshAllData(); }});
        btnPending.setOnAction(e -> { if(btnPending.isSelected()) { currentWishFilter = "PENDING"; wishTable.setPlaceholder(new Label("Onay bekleyen dileğin yok.")); refreshAllData(); }});
        
        filterBox.getChildren().addAll(lblFilter, btnApproved, btnPurchased, btnPending);
        
        GridPane formGrid = new GridPane(); formGrid.setHgap(20); formGrid.setVgap(15); formGrid.setPadding(new Insets(25)); formGrid.getStyleClass().add("form-card");
        Label lblWish = new Label("YENİ DİLEK EKLE:"); lblWish.getStyleClass().add("input-label");
        TextField wishNameField = new TextField(); wishNameField.setPromptText("Örn: Bisiklet, Yeni Oyun..."); wishNameField.getStyleClass().add("modern-input"); GridPane.setHgrow(wishNameField, Priority.ALWAYS);
        Button addBtn = new Button("Dileğimi Gönder ✨"); addBtn.getStyleClass().add("button-primary"); addBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 25;");
        formGrid.add(lblWish, 0, 0, 2, 1); formGrid.add(wishNameField, 0, 1); formGrid.add(addBtn, 1, 1);
        
        addBtn.setOnAction(e -> {
            String name = wishNameField.getText();
            if (name.isEmpty()) { showCustomDialog("ERROR", "Eksik Bilgi", "Lütfen ne istediğini yaz."); return; }
            wishService.requestWish(currentKid.getId(), name, 0);
            wishNameField.clear(); 
            showCustomDialog("SUCCESS", "Dileğin Yola Çıktı! 🚀", "Dileğin ebeveynine iletildi. 'Bekleyenler' sekmesinden takip edebilirsin.");
            refreshAllData();
        });
        
        wishTable = new TableView<>(); wishTable.setPlaceholder(new Label("Burada gösterecek bir şey yok."));
        
        // --- SÜTUNLAR ---
        TableColumn<Wish, String> wNameCol = new TableColumn<>("Dileğim"); 
        wNameCol.setCellValueFactory(data -> new SimpleStringProperty(formatText(data.getValue().getName())));

        TableColumn<Wish, String> wStatusCol = new TableColumn<>("Durum"); 
        wStatusCol.setStyle("-fx-alignment: CENTER;");
        wStatusCol.setCellValueFactory(data -> {
            Wish w = data.getValue();
            if (w.isPurchased()) return new SimpleStringProperty("ALINDI! 🎁");
            if (w.isApproved()) return new SimpleStringProperty("ONAYLANDI ✅");
            return new SimpleStringProperty("BEKLİYOR ⏳");
        });

        TableColumn<Wish, String> wCostCol = new TableColumn<>("Puan");
        wCostCol.setStyle("-fx-alignment: CENTER; -fx-font-weight:bold;");
        wCostCol.setCellValueFactory(data -> {
            Wish w = data.getValue();
            if (w.isApproved() || w.isPurchased()) return new SimpleStringProperty(w.getRequiredPoints() + " 💎");
            return new SimpleStringProperty("-");
        });

        TableColumn<Wish, String> wLevelCol = new TableColumn<>("Min Lvl");
        wLevelCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #8e44ad; -fx-font-weight:bold;");
        wLevelCol.setCellValueFactory(data -> {
            Wish w = data.getValue();
            if (w.isApproved() || w.isPurchased()) return new SimpleStringProperty("Lvl " + w.getRequiredLevel());
            return new SimpleStringProperty("-");
        });

        TableColumn<Wish, Void> wActionCol = new TableColumn<>("İşlem"); 
        wActionCol.setStyle("-fx-alignment: CENTER;");
        wActionCol.setCellFactory(param -> new TableCell<>() {
            private final Button buyBtn = new Button("Satın Al 🛒");
            {
                buyBtn.getStyleClass().add("button-secondary");
                buyBtn.setOnAction(e -> {
                    Wish w = getTableView().getItems().get(getIndex());
                    boolean success = wishService.purchaseWish(currentKid.getId(), w.getId());
                    if (success) { 
                        showCustomDialog("CELEBRATE", "TEBRİKLER! 🎉🎁", "Bunu başardın! Ödülün şimdi çantanda.");
                        refreshAllData(); 
                    }
                    else { 
                        User me = userService.getUserById(currentKid.getId());
                        if (me.getLevel() < w.getRequiredLevel()) {
                            showCustomDialog("ERROR", "Seviyen Yetmiyor 🔒", "Bu ödülü almak için Level " + w.getRequiredLevel() + " olmalısın.\nSen şu an Level " + me.getLevel() + " seviyesindesin.");
                        } else {
                            showCustomDialog("ERROR", "Yetersiz Bakiye 📉", "Bunu almak için " + (w.getRequiredPoints() - me.getPoints()) + " puana daha ihtiyacın var."); 
                        }
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); } else {
                    Wish w = getTableView().getItems().get(getIndex());
                    if (w.isPurchased()) { Label lbl = new Label("✅ Çantanda"); lbl.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); setGraphic(lbl); }
                    else if (!w.isApproved()) { Label lbl = new Label("Onay Bekleniyor..."); lbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;"); setGraphic(lbl); }
                    else {
                        User me = userService.getUserById(currentKid.getId());
                        if (me.getLevel() < w.getRequiredLevel()) {
                            Label lbl = new Label("🔒 Lvl " + w.getRequiredLevel() + " Gerekli");
                            lbl.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-background-color: #fab1a0; -fx-padding: 5 10; -fx-background-radius: 10;");
                            setGraphic(lbl);
                        } else {
                            int needed = w.getRequiredPoints() - me.getPoints();
                            if (needed <= 0) { buyBtn.setText("Satın Al (-" + w.getRequiredPoints() + ")"); setGraphic(buyBtn); }
                            else { Label lbl = new Label(needed + " Puan eksik"); lbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); setGraphic(lbl); }
                        }
                    }
                }
            }
        });

        wishTable.getColumns().addAll(wNameCol, wStatusCol, wCostCol, wLevelCol, wActionCol); 
        wishTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); 
        VBox.setVgrow(wishTable, Priority.ALWAYS);
        
        vbox.getChildren().addAll(filterBox, formGrid, wishTable);
        return vbox;
    }

    // --- TAB 3: LİDERLİK ---
    private VBox createLeaderboardTabContent() {
        VBox vbox = new VBox(20); vbox.setPadding(new Insets(20));
        HBox filterBox = new HBox(15); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblFilter = new Label("SIRALAMA:"); lblFilter.getStyleClass().add("filter-label");
        ToggleButton btnSiblings = new ToggleButton("Kardeşlerim 🏠"); ToggleButton btnClass = new ToggleButton("Sınıfım 🏫"); ToggleButton btnGlobal = new ToggleButton("Global (Dünya) 🌍");
        ToggleGroup lGroup = new ToggleGroup(); btnSiblings.setToggleGroup(lGroup); btnClass.setToggleGroup(lGroup); btnGlobal.setToggleGroup(lGroup); btnSiblings.setSelected(true);
        btnSiblings.getStyleClass().add("filter-toggle"); btnClass.getStyleClass().add("filter-toggle"); btnGlobal.getStyleClass().add("filter-toggle");
        
        btnSiblings.setOnAction(e -> { if(btnSiblings.isSelected()) { currentLeaderboardFilter = "SIBLINGS"; refreshAllData(); }});
        btnClass.setOnAction(e -> { if(btnClass.isSelected()) { currentLeaderboardFilter = "CLASS"; refreshAllData(); }});
        btnGlobal.setOnAction(e -> { if(btnGlobal.isSelected()) { currentLeaderboardFilter = "GLOBAL"; refreshAllData(); }});
        filterBox.getChildren().addAll(lblFilter, btnSiblings, btnClass, btnGlobal);
        
        leaderboardTitle = new Label("🏆 Kardeşler Liderlik Tablosu"); leaderboardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f1c40f;");
        leaderboardPane = new TilePane(); leaderboardPane.setHgap(20); leaderboardPane.setVgap(20); leaderboardPane.setPrefColumns(3);
        
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(leaderboardPane);
        scroller.getStyleClass().add("leader-scroll");
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setPrefViewportHeight(380);
        VBox.setVgrow(scroller, Priority.ALWAYS);
        vbox.getChildren().addAll(filterBox, leaderboardTitle, scroller);
        return vbox;
    }

    private VBox createLeaderCard(User u, int rank) {
        VBox card = new VBox(10); card.getStyleClass().add("card"); card.setMinWidth(220); card.setAlignment(Pos.CENTER);
        String rankIcon = switch (rank) { case 1 -> "🥇 1."; case 2 -> "🥈 2."; case 3 -> "🥉 3."; default -> rank + "."; };
        Label rankLbl = new Label(rankIcon); rankLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label nameLbl = new Label(formatText(u.getName())); nameLbl.getStyleClass().add("card-title");
        
        if ("GLOBAL".equals(currentLeaderboardFilter) || "CLASS".equals(currentLeaderboardFilter) || "SIBLINGS".equals(currentLeaderboardFilter)) {
            if (rank == 1) { rankLbl.getStyleClass().addAll("medal-gold", "medal-gold-rank"); nameLbl.getStyleClass().add("medal-gold-label"); } 
            else if (rank == 2) { rankLbl.getStyleClass().addAll("medal-silver", "medal-silver-rank"); nameLbl.getStyleClass().add("medal-silver-label"); } 
            else if (rank == 3) { rankLbl.getStyleClass().addAll("medal-bronze", "medal-bronze-rank"); nameLbl.getStyleClass().add("medal-bronze-label"); }
        }
        Label pointLbl = new Label(u.getTotalPoints() + " XP (Lvl " + u.getLevel() + ")"); pointLbl.getStyleClass().add("card-points"); pointLbl.setStyle("-fx-font-size: 18px;"); 
        Label classLbl = new Label(u.getClassId() != null ? u.getClassId() : "-"); classLbl.getStyleClass().add("card-level");
        card.getChildren().addAll(rankLbl, nameLbl, classLbl, pointLbl);
        if (u.getId().equals(currentKid.getId())) { card.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 30; -fx-background-radius: 30;"); }
        return card;
    }

    private void refreshAllData() {
        User updatedSelf = userService.getUserById(currentKid.getId());
        if (updatedSelf != null) { levelLabel.setText("Lvl " + updatedSelf.getLevel()); pointsLabel.setText(updatedSelf.getPoints() + " 💰"); }
        
        List<Task> myTasks = taskService.getTasksOfKid(currentKid.getId()).stream().filter(t -> t.getStatus().equalsIgnoreCase(currentTaskFilter)).collect(Collectors.toList());
        taskTable.setItems(FXCollections.observableArrayList(myTasks));
        
        List<Wish> myWishes = wishService.getWishesOfKid(currentKid.getId());
        List<Wish> filteredWishes = myWishes.stream().filter(w -> {
            switch (currentWishFilter) {
                case "APPROVED": return w.isApproved() && !w.isPurchased();
                case "PURCHASED": return w.isPurchased();
                case "PENDING": return !w.isApproved();
                default: return false;
            }
        }).collect(Collectors.toList());
        wishTable.setItems(FXCollections.observableArrayList(filteredWishes));
        
        leaderboardPane.getChildren().clear();
        List<User> usersToShow;
        switch (currentLeaderboardFilter) {
            case "CLASS": usersToShow = userService.getKidsOfClass(currentKid.getClassId()); leaderboardTitle.setText("🏫 Sınıf Arkadaşları"); break;
            case "GLOBAL": usersToShow = userService.getAllKids(); leaderboardTitle.setText("🌍 Dünya Geneli Sıralama"); break;
            case "SIBLINGS": default:
                java.util.List<String> pids = currentKid.getParentIds() == null ? java.util.List.of() : currentKid.getParentIds();
                usersToShow = userService.getChildrenOfParents(pids);
                leaderboardTitle.setText("🏠 Kardeşler Arası Sıralama");
                break;
        }
        usersToShow.sort(Comparator.comparingInt(User::getTotalPoints).reversed());
        int rank = 1; for (User u : usersToShow) { leaderboardPane.getChildren().add(createLeaderCard(u, rank++)); }
    }

    // MODERN POP-UP (SAHİPLİK DÜZELTİLDİ)
    private void showCustomDialog(String type, String title, String message) {
        Stage dialog = new Stage(); 
        dialog.initModality(Modality.APPLICATION_MODAL); 
        dialog.initStyle(StageStyle.TRANSPARENT);
        
        // Pencere sahipliği ayarı: Popup'ın ana pencereye bağlı olması sağlanır
        if (dashboardStage != null) {
            dialog.initOwner(dashboardStage);
        }

        VBox card = new VBox(15); card.setAlignment(Pos.CENTER); card.getStyleClass().add("custom-dialog");
        String icon = "ⓘ"; String btnClass = "dialog-btn";
        if (type.equals("SUCCESS")) { icon = "✅"; } else if (type.equals("ERROR")) { icon = "⚠"; btnClass = "dialog-btn-error"; } else if (type.equals("CELEBRATE")) { icon = "🎉"; btnClass = "dialog-btn-celebrate"; }
        Label iconLbl = new Label(icon); iconLbl.getStyleClass().add("dialog-icon");
        Label titleLbl = new Label(title); titleLbl.getStyleClass().add("dialog-title");
        Label msgLbl = new Label(message); msgLbl.getStyleClass().add("dialog-message"); msgLbl.setWrapText(true);
        Button closeBtn = new Button("Tamam"); closeBtn.getStyleClass().add("dialog-btn"); if (!"dialog-btn".equals(btnClass)) { closeBtn.getStyleClass().add(btnClass); }
        closeBtn.setOnAction(e -> dialog.close()); card.getChildren().addAll(iconLbl, titleLbl, msgLbl, closeBtn);
        
        StackPane root = new StackPane(card); root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) { }
        dialog.setScene(scene); dialog.showAndWait();
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        java.util.Locale trLocale = java.util.Locale.forLanguageTag("tr");
        for (String w : words) { if (w.length() > 0) { sb.append(w.substring(0, 1).toUpperCase(trLocale)); if (w.length() > 1) sb.append(w.substring(1).toLowerCase(trLocale)); sb.append(" "); } }
        return sb.toString().trim();
    }
}