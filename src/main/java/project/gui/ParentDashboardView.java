package project.gui;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
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
import javafx.util.StringConverter;
import project.model.Task;
import project.model.User;
import project.model.Wish;
import project.service.TaskService;
import project.service.UserService;
import project.service.WishService;

public class ParentDashboardView {

    private final User currentParent;
    private final UserService userService = new UserService();
    private final TaskService taskService;
    private final WishService wishService;

    // UI Elemanları
    private TableView<Task> taskTable;
    private TableView<Wish> wishTable;
    private TilePane kidsCardPane;
    private TilePane leaderboardPane;
    private Label leaderboardTitle;

    private ComboBox<User> kidSelectorForTaskAssign;
    private ComboBox<User> taskChildFilterCombo;
    private ComboBox<User> wishChildFilterCombo;
    
    private boolean isRefreshing = false; 
    private User sharedChildFilter = null;
    private String currentTaskStatusFilter = "PENDING"; 
    private String currentWishStatusFilter = "PENDING"; 

    public ParentDashboardView(User parent) {
        this.currentParent = parent;
        this.taskService = new TaskService(userService);
        this.wishService = new WishService(userService);
    }

    public void show() {
        Stage stage = new Stage();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // --- HEADER ---
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.getStyleClass().add("top-header");

        Label appTitle = new Label("KidTask Ebeveyn");
        appTitle.getStyleClass().add("app-title");

        HBox userBadgeBox = new HBox(15);
        userBadgeBox.setAlignment(Pos.CENTER);
        userBadgeBox.getStyleClass().add("user-tag");
        
        Label nameLbl = new Label("👋 Hoş Geldin, " + formatText(currentParent.getName()));
        nameLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: #7f8c8d;");
        
        

        userBadgeBox.getChildren().addAll(nameLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("⟳ Yenile");
        refreshBtn.getStyleClass().add("button-refresh");
        refreshBtn.setOnAction(e -> refreshAllData());

        Button logoutBtn = new Button("Çıkış Yap");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setOnAction(e -> {
            stage.close();
            new LoginView().show(new Stage());
        });

        topBar.getChildren().addAll(appTitle, userBadgeBox, spacer, refreshBtn, logoutBtn);
        root.setTop(topBar);

        // --- TABS ---
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab kidsTab = new Tab("Çocuklarım 🏠", createKidsTabContent());
        Tab tasksTab = new Tab("Görev Yönetimi 📋", createTaskTabContent());
        Tab wishesTab = new Tab("Dilek Onayları 🎁", createWishTabContent());
        Tab leaderTab = new Tab("Liderlik Tablosu 🏆", createLeaderboardTabContent());

        tabPane.getTabs().addAll(kidsTab, tasksTab, wishesTab, leaderTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1100, 750);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) { }
        
        stage.setTitle("KidTask - Ebeveyn Paneli");
        stage.setScene(scene);
        stage.show();
        
        refreshAllData();
    }

    // ================== TAB 1: KIDS ==================
    private VBox createKidsTabContent() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));

        // YENİ ÇOCUK EKLEME KARTI
        HBox addKidBox = new HBox(15);
        addKidBox.setAlignment(Pos.CENTER_LEFT);
        addKidBox.getStyleClass().add("filter-pane");

        Label lbl = new Label("YENİ ÇOCUK BAĞLA:");
        lbl.getStyleClass().add("filter-label");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Kullanıcı Adı (Örn: Beyza)");
        usernameField.getStyleClass().add("modern-input");

        Button addBtn = new Button("+ Ekle");
        addBtn.getStyleClass().add("button-secondary");

        addBtn.setOnAction(e -> {
            String targetUsername = usernameField.getText().trim();
            if (targetUsername.isEmpty()) { showCustomDialog("ERROR", "Uyarı", "Kullanıcı adı giriniz."); return; }
            boolean success = userService.linkChildByUsername(currentParent, targetUsername);
            if (success) {
                showCustomDialog("SUCCESS", "Başarılı", targetUsername + " eklendi! 🎉");
                usernameField.clear();
                refreshAllData();
            } else {
                showCustomDialog("ERROR", "Hata", "Kullanıcı bulunamadı veya zaten ekli.");
            }
        });

        addKidBox.getChildren().addAll(lbl, usernameField, addBtn);

        Label cardsTitle = new Label("Çocuklarımın Durumu");
        cardsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #34495e;");

        kidsCardPane = new TilePane();
        kidsCardPane.setHgap(20); kidsCardPane.setVgap(20); kidsCardPane.setPrefColumns(3);

        vbox.getChildren().addAll(addKidBox, cardsTitle, kidsCardPane);
        return vbox;
    }

    private VBox createKidCard(User kid) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMinWidth(250);
        card.setAlignment(Pos.CENTER);

        Label nameLbl = new Label(formatText(kid.getName()) + " " + formatText(kid.getSurname()));
        nameLbl.getStyleClass().add("card-title");
        
        // Puan ve Level Gösterimi
        Label pointsLbl = new Label(kid.getPoints() + " 💰 (Cüzdan)");
        pointsLbl.getStyleClass().add("card-points");
        pointsLbl.setStyle("-fx-font-size: 24px; -fx-text-fill: #27ae60;");
        
        Label levelLbl = new Label("Level " + kid.getLevel() + " (" + kid.getTotalPoints() + " XP)");
        levelLbl.getStyleClass().add("card-level");
        
        Label infoLbl = new Label("@" + kid.getName());
        infoLbl.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");

        card.getChildren().addAll(nameLbl, pointsLbl, levelLbl, new Separator(), infoLbl);
        return card;
    }

    // --- TAB: LIDERLIK ---
    private VBox createLeaderboardTabContent() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        HBox filterBox = new HBox(15); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblFilter = new Label("SIRALAMA:"); lblFilter.getStyleClass().add("filter-label");
        ToggleButton btnChildren = new ToggleButton("Çocuklarım 🏠"); ToggleButton btnGlobal = new ToggleButton("Global (Dünya) 🌍");
        ToggleGroup g = new ToggleGroup(); btnChildren.setToggleGroup(g); btnGlobal.setToggleGroup(g); btnChildren.setSelected(true);
        btnChildren.getStyleClass().add("filter-toggle"); btnGlobal.getStyleClass().add("filter-toggle");
        filterBox.getChildren().addAll(lblFilter, btnChildren, btnGlobal);

        leaderboardTitle = new Label("🏠 Çocuklarım Sıralaması"); leaderboardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f1c40f;");
        leaderboardPane = new TilePane(); leaderboardPane.setHgap(20); leaderboardPane.setVgap(20); leaderboardPane.setPrefColumns(3);
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(leaderboardPane);
        scroller.setFitToWidth(true); scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED); scroller.getStyleClass().add("leader-scroll");
        VBox.setVgrow(scroller, Priority.ALWAYS);

        btnChildren.setOnAction(e -> { if (btnChildren.isSelected()) { leaderboardTitle.setText("🏠 Çocuklarım Sıralaması"); refreshAllData(); }});
        btnGlobal.setOnAction(e -> { if (btnGlobal.isSelected()) { leaderboardTitle.setText("🌍 Global Sıralama"); refreshAllData(); }});

        vbox.getChildren().addAll(filterBox, leaderboardTitle, scroller);
        return vbox;
    }

    private VBox createLeaderCard(User u, int rank) {
        VBox card = new VBox(10); card.getStyleClass().add("card"); card.setMinWidth(220); card.setAlignment(Pos.CENTER);
        String rankIcon = switch (rank) { case 1 -> "🥇 1."; case 2 -> "🥈 2."; case 3 -> "🥉 3."; default -> rank + "."; };
        Label rankLbl = new Label(rankIcon); rankLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label nameLbl = new Label(formatText(u.getName())); nameLbl.getStyleClass().add("card-title");
        if (rank == 1) { rankLbl.getStyleClass().addAll("medal-gold", "medal-gold-rank"); nameLbl.getStyleClass().add("medal-gold-label"); }
        else if (rank == 2) { rankLbl.getStyleClass().addAll("medal-silver", "medal-silver-rank"); nameLbl.getStyleClass().add("medal-silver-label"); }
        else if (rank == 3) { rankLbl.getStyleClass().addAll("medal-bronze", "medal-bronze-rank"); nameLbl.getStyleClass().add("medal-bronze-label"); }
        Label pointLbl = new Label(u.getTotalPoints() + " XP (Lvl " + u.getLevel() + ")"); pointLbl.getStyleClass().add("card-points"); pointLbl.setStyle("-fx-font-size: 18px;");
        Label classLbl = new Label(u.getClassId() != null ? u.getClassId() : "-"); classLbl.getStyleClass().add("card-level");
        card.getChildren().addAll(rankLbl, nameLbl, classLbl, pointLbl);
        return card;
    }

    // ================== TAB 2: TASKS ==================
    private VBox createTaskTabContent() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        // FİLTRELER
        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("filter-pane");

        Label lblStatus = new Label("DURUM:");
        lblStatus.getStyleClass().add("filter-label");

        ToggleButton btnTodo = new ToggleButton("Yapılacaklar");
        ToggleButton btnPending = new ToggleButton("Onay Bekleyenler ⏳");
        ToggleButton btnCompleted = new ToggleButton("Tamamlananlar ✅");
        
        ToggleGroup group = new ToggleGroup();
        btnTodo.setToggleGroup(group); btnPending.setToggleGroup(group); btnCompleted.setToggleGroup(group);
        btnPending.setSelected(true);

        btnTodo.getStyleClass().add("filter-toggle");
        btnPending.getStyleClass().add("filter-toggle");
        btnCompleted.getStyleClass().add("filter-toggle");

        btnTodo.setOnAction(e -> { if(btnTodo.isSelected()) { currentTaskStatusFilter = "TODO"; refreshAllData(); }});
        btnPending.setOnAction(e -> { if(btnPending.isSelected()) { currentTaskStatusFilter = "PENDING"; refreshAllData(); }});
        btnCompleted.setOnAction(e -> { if(btnCompleted.isSelected()) { currentTaskStatusFilter = "COMPLETED"; refreshAllData(); }});

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        Label lblChild = new Label("ÇOCUK:");
        lblChild.getStyleClass().add("filter-label");

        taskChildFilterCombo = new ComboBox<>();
        taskChildFilterCombo.setPromptText("Tümü");
        taskChildFilterCombo.setConverter(getUserStringConverter());
        taskChildFilterCombo.getStyleClass().add("filter-combo");
        
        taskChildFilterCombo.setOnAction(e -> {
            if (isRefreshing) return;
            sharedChildFilter = taskChildFilterCombo.getValue();
            refreshAllData();
        });

        Button clearKidFilterBtn = new Button("X");
        clearKidFilterBtn.getStyleClass().add("filter-clear-btn");
        clearKidFilterBtn.setTooltip(new Tooltip("Filtreyi Temizle"));
        clearKidFilterBtn.setOnAction(e -> { sharedChildFilter = null; refreshAllData(); });

        filterBox.getChildren().addAll(lblStatus, btnTodo, btnPending, btnCompleted, sep, lblChild, taskChildFilterCombo, clearKidFilterBtn);

      taskTable = new TableView<>();
        taskTable.setPlaceholder(new Label("Görev bulunamadı 📭"));
        
        TableColumn<Task, String> kidNameCol = new TableColumn<>("ÇOCUK");
        kidNameCol.setPrefWidth(150); // Sabit Genişlik
        kidNameCol.setCellValueFactory(data -> {
            User kid = userService.getUserById(data.getValue().getKidId());
            return new SimpleStringProperty(kid != null ? formatText(kid.getName()) : "Bilinmiyor");
        });

        TableColumn<Task, String> taskNameCol = new TableColumn<>("GÖREV ADI");
        taskNameCol.setPrefWidth(250); // Geniş bıraktık
        taskNameCol.setCellValueFactory(data -> new SimpleStringProperty(formatText(data.getValue().getTaskName())));

        TableColumn<Task, Integer> pointsCol = new TableColumn<>("ÖDÜL");
        pointsCol.setPrefWidth(100);
        pointsCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPoints()));
        pointsCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Task, Void> actionCol = new TableColumn<>("İŞLEM");
        actionCol.setMinWidth(200);
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Onayla ✅");
            {
                btn.getStyleClass().add("button-secondary");
                btn.setStyle("-fx-font-size: 13px; -fx-padding: 6 15;");
                btn.setOnAction(event -> {
                    Task t = getTableView().getItems().get(getIndex());
                    taskService.approveTask(t.getId());
                    refreshAllData();
                    showCustomDialog("SUCCESS", "Başarılı", "Puan gönderildi! 🚀");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Task t = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(t.getStatus())) setGraphic(btn);
                    else if ("TODO".equals(t.getStatus())) { Label l = new Label("Yapılıyor..."); l.setStyle("-fx-text-fill: #3498db; -fx-font-weight:bold;"); setGraphic(l); }
                    else { Label l = new Label("Tamamlandı"); l.setStyle("-fx-text-fill: #27ae60; -fx-font-weight:bold;"); setGraphic(l); }
                }
            }
        });

       taskTable.getColumns().addAll(kidNameCol, taskNameCol, pointsCol, actionCol);
       taskTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(taskTable, Priority.ALWAYS);

        TitledPane addPane = new TitledPane("Yeni Görev Ata (+)", createAddTaskForm());
        addPane.setExpanded(false);

        VBox.setVgrow(taskTable, Priority.ALWAYS);
        vbox.getChildren().addAll(filterBox, taskTable, addPane);
        return vbox;
    }

    private GridPane createAddTaskForm() {
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20); 
        formGrid.setVgap(15); 
        formGrid.setPadding(new Insets(20));
        formGrid.getStyleClass().add("form-card"); 

        // 1. Satır: Kime ve Görev Adı
        Label lblWho = new Label("KİME ATANACAK?"); lblWho.getStyleClass().add("input-label");
        kidSelectorForTaskAssign = new ComboBox<>();
        kidSelectorForTaskAssign.setPromptText("Çocuk Seçiniz...");
        kidSelectorForTaskAssign.setConverter(getUserStringConverter());
        kidSelectorForTaskAssign.setMaxWidth(Double.MAX_VALUE);
        kidSelectorForTaskAssign.getStyleClass().add("modern-input");
        
        Label lblTask = new Label("GÖREV ADI?"); lblTask.getStyleClass().add("input-label");
        TextField taskNameField = new TextField(); 
        taskNameField.setPromptText("Örn: Odanı Topla...");
        taskNameField.getStyleClass().add("modern-input");

        formGrid.add(lblWho, 0, 0); formGrid.add(kidSelectorForTaskAssign, 0, 1);
        formGrid.add(lblTask, 1, 0); formGrid.add(taskNameField, 1, 1);

        // 2. Satır: Puan ve Tarih
        Label lblPoint = new Label("PUAN DEĞERİ?"); lblPoint.getStyleClass().add("input-label");
        TextField pointsField = new TextField(); 
        pointsField.setPromptText("50");
        pointsField.getStyleClass().add("modern-input");

        Label lblDate = new Label("SON TARİH?"); lblDate.getStyleClass().add("input-label");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.getStyleClass().add("modern-input");

        formGrid.add(lblPoint, 0, 2); formGrid.add(pointsField, 0, 3);
        formGrid.add(lblDate, 1, 2); formGrid.add(datePicker, 1, 3);

        // 3. Satır: Açıklama Alanı (DÜZELTME: TEXTFIELD YAPILDI)
        Label lblDesc = new Label("AÇIKLAMA"); 
        lblDesc.getStyleClass().add("input-label");
        
        // TextArea yerine TextField yaptık, görüntü %100 diğerleriyle aynı oldu
        TextField descField = new TextField();
        descField.setPromptText("Detaylı bilgi (Opsiyonel)");
        descField.getStyleClass().add("modern-input");

        // Genişliği doldursun diye 2 sütuna yayıyoruz (colspan: 2)
        formGrid.add(lblDesc, 0, 4, 2, 1); 
        formGrid.add(descField, 0, 5, 2, 1);

        // 4. Satır: Buton
        Button addBtn = new Button("Görevi Oluştur (+)");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-font-size: 14px; -fx-padding: 12 20;");

        formGrid.add(addBtn, 0, 6, 2, 1); 

        addBtn.setOnAction(e -> {
            try {
                User k = kidSelectorForTaskAssign.getValue();
                String n = taskNameField.getText();
                String d = descField.getText(); // TextField verisi alınıyor
                
                if (d == null || d.trim().isEmpty()) d = "Açıklama yok.";

                if (k == null || n.isEmpty() || pointsField.getText().isEmpty()) { 
                    showCustomDialog("ERROR", "Eksik Bilgi", "Lütfen zorunlu alanları doldurunuz."); return; 
                }
                
                int p = Integer.parseInt(pointsField.getText());
                
                taskService.addTask(k.getId(), n, d, p, datePicker.getValue(), "General", currentParent.getName());
                
                taskNameField.clear(); 
                pointsField.clear();
                descField.clear(); 
                
                refreshAllData();
                showCustomDialog("SUCCESS", "Görev Atandı", k.getName() + " için yeni görev hazır.");
            } catch (NumberFormatException ex) { 
                showCustomDialog("ERROR", "Hata", "Puan sadece sayı olmalıdır."); 
            }
        });
        return formGrid;
    }

    // ================== TAB 3: WISHES (LEVEL & PUAN KONTROLÜ) ==================
    private VBox createWishTabContent() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("filter-pane");

        Label lblStatus = new Label("DURUM:");
        lblStatus.getStyleClass().add("filter-label");

        ToggleButton btnPending = new ToggleButton("Onay Bekleyen");
        ToggleButton btnApproved = new ToggleButton("Onaylananlar");
        ToggleButton btnPurchased = new ToggleButton("Satın Alınanlar");
        
        ToggleGroup wGroup = new ToggleGroup();
        btnPending.setToggleGroup(wGroup); btnApproved.setToggleGroup(wGroup); btnPurchased.setToggleGroup(wGroup);
        btnPending.setSelected(true); 

        btnPending.getStyleClass().add("filter-toggle");
        btnApproved.getStyleClass().add("filter-toggle");
        btnPurchased.getStyleClass().add("filter-toggle");

        btnPending.setOnAction(e -> { if(btnPending.isSelected()) { currentWishStatusFilter = "PENDING"; refreshAllData(); }});
        btnApproved.setOnAction(e -> { if(btnApproved.isSelected()) { currentWishStatusFilter = "APPROVED"; refreshAllData(); }});
        btnPurchased.setOnAction(e -> { if(btnPurchased.isSelected()) { currentWishStatusFilter = "PURCHASED"; refreshAllData(); }});

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        Label lblChild = new Label("ÇOCUK:"); lblChild.getStyleClass().add("filter-label");

        wishChildFilterCombo = new ComboBox<>();
        wishChildFilterCombo.setPromptText("Tümü");
        wishChildFilterCombo.setConverter(getUserStringConverter());
        wishChildFilterCombo.getStyleClass().add("filter-combo");

        wishChildFilterCombo.setOnAction(e -> {
            if (isRefreshing) return;
            sharedChildFilter = wishChildFilterCombo.getValue();
            refreshAllData();
        });

        Button clearWishKidBtn = new Button("X");
        clearWishKidBtn.getStyleClass().add("filter-clear-btn");
        clearWishKidBtn.setOnAction(e -> { sharedChildFilter = null; refreshAllData(); });

        filterBox.getChildren().addAll(lblStatus, btnPending, btnApproved, btnPurchased, sep, lblChild, wishChildFilterCombo, clearWishKidBtn);

       wishTable = new TableView<>();
        
        TableColumn<Wish, String> wKidCol = new TableColumn<>("ÇOCUK");
        wKidCol.setPrefWidth(150);
        wKidCol.setCellValueFactory(data -> {
            User kid = userService.getUserById(data.getValue().getKidId());
            return new SimpleStringProperty(kid != null ? formatText(kid.getName()) : "?");
        });

       TableColumn<Wish, String> wNameCol = new TableColumn<>("DİLEK İSTEĞİ");
        wNameCol.setPrefWidth(250); // İsimler için geniş alan
        wNameCol.setCellValueFactory(data -> new SimpleStringProperty(formatText(data.getValue().getName())));
        
        TableColumn<Wish, String> wStatusCol = new TableColumn<>("MEVCUT BİLGİ");
        wStatusCol.setPrefWidth(180);
        wStatusCol.setStyle("-fx-alignment: CENTER;");
        wStatusCol.setCellValueFactory(data -> {
            Wish w = data.getValue();
            if(w.isPurchased()) return new SimpleStringProperty("SATIN ALINDI 🎁");
            if(w.isApproved()) return new SimpleStringProperty(w.getRequiredPoints() + " Puan | Lvl " + w.getRequiredLevel());
            return new SimpleStringProperty("ONAY BEKLİYOR ⏳");
        });

        TableColumn<Wish, Void> wActionCol = new TableColumn<>("ONAY AYARLARI");
        wActionCol.setMinWidth(380); // Kutular ve butonlar sığsın diye genişlettik
        
        wActionCol.setCellFactory(param -> new TableCell<>() {
            private final TextField txtPoints = new TextField();
            private final TextField txtLevel = new TextField();
            private final Button btnConfirm = new Button("Onayla");
            private final HBox pane = new HBox(12);

            {
                txtPoints.setPromptText("Puan"); txtPoints.setPrefWidth(90);
                txtLevel.setPromptText("Lvl"); txtLevel.setPrefWidth(60);
                btnConfirm.getStyleClass().add("button-primary");
                pane.setAlignment(Pos.CENTER_LEFT);
                pane.getChildren().addAll(new Label("Puan:"), txtPoints, new Label("Lvl:"), txtLevel, btnConfirm);
                btnConfirm.setOnAction(event -> {
                    Wish w = getTableView().getItems().get(getIndex());
                    try {
                        int cost = Integer.parseInt(txtPoints.getText());
                        // Level boş bırakılırsa 1 olsun
                        int level = txtLevel.getText().isEmpty() ? 1 : Integer.parseInt(txtLevel.getText());
                        
                        // ONAYLA (Puan + Level)
                        wishService.approveWish(w.getId(), cost, level);
                        
                        refreshAllData();
                        showCustomDialog("SUCCESS", "İstek Onaylandı", "Bu dilek için " + cost + " Puan ve Level " + level + " şartı koydunuz.");
                    } catch (NumberFormatException e) {
                        showCustomDialog("ERROR", "Hatalı Giriş", "Lütfen Puan ve Level alanlarına sadece sayı giriniz.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Wish w = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(currentWishStatusFilter)) {
                        // Eğer çocuk puan önerdiyse kutuya yaz
                        if (txtPoints.getText().isEmpty() && w.getRequiredPoints() > 0) {
                            txtPoints.setText(String.valueOf(w.getRequiredPoints()));
                        }
                        setGraphic(pane); // Kutuları göster
                    } else {
                        setGraphic(null); // Zaten onaylıysa gösterme
                    }
                }
            }
        });

        wishTable.getColumns().addAll(wKidCol, wNameCol, wStatusCol, wActionCol);
        wishTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(wishTable, Priority.ALWAYS);

        vbox.getChildren().addAll(filterBox, wishTable);
        return vbox;
    }

    private void refreshAllData() {
        isRefreshing = true;
        try {
                List<User> myKids = userService.getAllUsers().stream()
                    .filter(u -> "kid".equalsIgnoreCase(u.getRole()))
                    .filter(u -> (u.getParentIds() != null && u.getParentIds().contains(currentParent.getId())) || (currentParent.getChildrenIds() != null && currentParent.getChildrenIds().contains(u.getId())))
                    .collect(Collectors.toList());

            kidsCardPane.getChildren().clear();
            for (User kid : myKids) {
                kidsCardPane.getChildren().add(createKidCard(kid));
            }

            updateComboSafely(kidSelectorForTaskAssign, myKids, null);
            updateComboSafely(taskChildFilterCombo, myKids, sharedChildFilter);
            updateComboSafely(wishChildFilterCombo, myKids, sharedChildFilter);

            // TASKS FILTER
            List<String> taskKidIds = (sharedChildFilter != null) ? List.of(sharedChildFilter.getId()) 
                                                                  : myKids.stream().map(User::getId).collect(Collectors.toList());

            List<Task> filteredTasks = taskService.getAllTasks().stream()
                    .filter(t -> taskKidIds.contains(t.getKidId())) 
                    .filter(t -> t.getStatus().equalsIgnoreCase(currentTaskStatusFilter))
                    .collect(Collectors.toList());
            taskTable.setItems(FXCollections.observableArrayList(filteredTasks));

            // WISHES FILTER
            List<String> wishKidIds = (sharedChildFilter != null) ? List.of(sharedChildFilter.getId()) 
                                                                  : myKids.stream().map(User::getId).collect(Collectors.toList());

            var allWishes = FXCollections.<Wish>observableArrayList();
            for (String kId : wishKidIds) allWishes.addAll(wishService.getWishesOfKid(kId));

            List<Wish> filteredWishes = allWishes.stream()
                    .filter(w -> {
                        if ("PURCHASED".equals(currentWishStatusFilter)) return w.isPurchased();
                        if ("APPROVED".equals(currentWishStatusFilter)) return w.isApproved() && !w.isPurchased();
                        if ("PENDING".equals(currentWishStatusFilter)) return !w.isApproved();
                        return true;
                    })
                    .collect(Collectors.toList());
            wishTable.setItems(FXCollections.observableArrayList(filteredWishes));

            // --- LEADERBOARD ---
            if (leaderboardPane != null) {
                leaderboardPane.getChildren().clear();
                List<User> usersToShow = null;
                // CHILDREN: show myKids, GLOBAL: all kids
                String filter = (leaderboardTitle != null && leaderboardTitle.getText() != null && leaderboardTitle.getText().contains("Global")) ? "GLOBAL" : "CHILDREN";
                if ("GLOBAL".equals(filter)) usersToShow = userService.getAllKids();
                else usersToShow = myKids;
                usersToShow.sort(java.util.Comparator.comparingInt(User::getTotalPoints).reversed());
                int rank = 1; for (User u : usersToShow) { leaderboardPane.getChildren().add(createLeaderCard(u, rank++)); }
            }

        } finally {
            isRefreshing = false;
        }
    }

    private void updateComboSafely(ComboBox<User> combo, List<User> newItems, User targetSelection) {
        if(combo == null) return;
        combo.setItems(FXCollections.observableArrayList(newItems));
        if (targetSelection != null) {
            for (User u : newItems) {
                if (u.getId().equals(targetSelection.getId())) {
                    combo.setValue(u);
                    return;
                }
            }
        } else {
            combo.setValue(null);
        }
    }

    private StringConverter<User> getUserStringConverter() {
        return new StringConverter<>() {
            @Override public String toString(User user) { return user == null ? null : user.getName(); }
            @Override public User fromString(String string) { return null; }
        };
    }

    // =========================================================================
    //   YENİ: MODERN POP-UP METODU (GÖLGE DÜZELTİLMİŞ)
    // =========================================================================
    private void showCustomDialog(String type, String title, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        // 1. İÇERİK KUTUSU
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("custom-dialog");

        String icon = "ⓘ";
        String btnClass = "dialog-btn";
        
        if (type.equals("SUCCESS")) { icon = "✅"; }
        else if (type.equals("ERROR")) { icon = "⚠"; btnClass = "dialog-btn-error"; }
        else if (type.equals("CELEBRATE")) { icon = "🎁🎉"; btnClass = "dialog-btn-celebrate"; }

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dialog-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-title");

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("dialog-message");
        msgLbl.setWrapText(true);

        Button closeBtn = new Button("Tamam");
        closeBtn.getStyleClass().add("dialog-btn");
        if (!"dialog-btn".equals(btnClass)) closeBtn.getStyleClass().add(btnClass);
        
        closeBtn.setOnAction(e -> dialog.close());

        card.getChildren().addAll(iconLbl, titleLbl, msgLbl, closeBtn);

        // 2. DIŞ ÇERÇEVE (Gölge Payı İçin)
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;"); 

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) { }

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        java.util.Locale trLocale = java.util.Locale.forLanguageTag("tr");
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(w.substring(0, 1).toUpperCase(trLocale));
                if (w.length() > 1) sb.append(w.substring(1).toLowerCase(trLocale));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}