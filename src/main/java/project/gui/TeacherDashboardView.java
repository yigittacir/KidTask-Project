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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
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
import project.service.TaskService;
import project.service.UserService;

public class TeacherDashboardView {

    private final User currentTeacher;
    private final UserService userService = new UserService();
    private final TaskService taskService;

    // UI
    private TilePane classesPane;
    private TableView<Task> taskTable;
    private TilePane classChipPane;
    private ComboBox<User> studentSelector;
    private String selectedClassFilter = null;
    private TilePane studentsPane;
    private VBox studentsContainer;
    private TilePane leaderboardPane;
    private Label leaderboardTitle;

    public TeacherDashboardView(User teacher) {
        this.currentTeacher = teacher;
        this.taskService = new TaskService(userService);
    }

    public void show() {
        Stage stage = new Stage();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Header
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.getStyleClass().add("top-header");

        Label appTitle = new Label("KidTask - Öğretmen");
        appTitle.getStyleClass().add("app-title");

        HBox userBadgeBox = new HBox(15);
        userBadgeBox.setAlignment(Pos.CENTER);
        userBadgeBox.getStyleClass().add("user-tag");

        Label nameLbl = new Label("👋 Hoş Geldin, " + formatText(currentTeacher.getName()));
        nameLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: #7f8c8d;");

        Label classesLbl = new Label((currentTeacher.getClassIds() == null ? 0 : currentTeacher.getClassIds().size()) + " sınıfı yönetiyorsun");
        classesLbl.setStyle("-fx-text-fill: #95a5a6;");

        userBadgeBox.getChildren().addAll(nameLbl, new Separator(javafx.geometry.Orientation.VERTICAL), classesLbl);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = new Button("⟳ Yenile"); refreshBtn.getStyleClass().add("button-refresh"); refreshBtn.setOnAction(e -> refreshAllData());
        Button logoutBtn = new Button("Çıkış Yap"); logoutBtn.getStyleClass().add("button-danger"); logoutBtn.setOnAction(e -> { stage.close(); new LoginView().show(new Stage()); });

        topBar.getChildren().addAll(appTitle, userBadgeBox, spacer, refreshBtn, logoutBtn);
        root.setTop(topBar);

        TabPane tabPane = new TabPane(); tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab classesTab = new Tab("Sınıflarım 🏫", createClassesTabContent());
        Tab tasksTab = new Tab("Görev Yönetimi 📚", createTaskTabContent());
        Tab leaderTab = new Tab("Liderlik Tablosu 🏆", createLeaderboardTabContent());
        tabPane.getTabs().addAll(classesTab, tasksTab, leaderTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1100, 750);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) {}
        stage.setTitle("KidTask - Öğretmen Paneli");
        stage.setScene(scene);
        stage.show();

        refreshAllData();
    }

    private VBox createClassesTabContent() {
        VBox v = new VBox(20); v.setPadding(new Insets(20));
        Label title = new Label("Sınıflarım"); title.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:#34495e;");
        classesPane = new TilePane(); classesPane.setHgap(20); classesPane.setVgap(20); classesPane.setPrefColumns(3);

        // students container (hidden initially) - shows when a class is clicked
        studentsPane = new TilePane(); studentsPane.setHgap(20); studentsPane.setVgap(20); studentsPane.setPrefColumns(3);
        Label studentsTitle = new Label(); studentsTitle.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:#34495e;");
        Button studentsBackBtn = new Button("Geri"); studentsBackBtn.getStyleClass().add("button-secondary"); studentsBackBtn.setStyle("-fx-font-size:13px; -fx-padding:6 12;");
        studentsBackBtn.setOnAction(e -> {
            // show classes, hide students
            classesPane.setVisible(true); classesPane.setManaged(true);
            studentsContainer.setVisible(false); studentsContainer.setManaged(false);
        });
        HBox studentsHeader = new HBox(12, studentsTitle, studentsBackBtn); studentsHeader.setAlignment(Pos.CENTER_LEFT);
        studentsContainer = new VBox(12, studentsHeader, studentsPane); studentsContainer.setVisible(false); studentsContainer.setManaged(false);

        // Add new class form
        HBox addBox = new HBox(12); addBox.setAlignment(Pos.CENTER_LEFT); addBox.getStyleClass().add("filter-pane");
        TextField classField = new TextField(); classField.setPromptText("Yeni sınıf kodu (Örn: 3A)"); classField.getStyleClass().add("modern-input");
        Button addBtn = new Button("+ Sınıf Ekle"); addBtn.getStyleClass().add("button-secondary");
        addBtn.setOnAction(e -> {
            String c = classField.getText().trim(); if (c.isEmpty()) { showCustomDialog("ERROR", "Hata", "Sınıf kodu girin."); return; }
            currentTeacher.addClass(c); userService.updateUser(currentTeacher); classField.clear(); refreshAllData(); showCustomDialog("SUCCESS", "Eklendi", c + " sınıfı eklendi.");
        });
        addBox.getChildren().addAll(new Label("Yeni Sınıf:"), classField, addBtn);

        v.getChildren().addAll(title, addBox, classesPane, studentsContainer);
        return v;
    }

    private VBox createTaskTabContent() {
        VBox v = new VBox(15); v.setPadding(new Insets(20));

        HBox filterBox = new HBox(12); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblClass = new Label("Sınıf:"); lblClass.getStyleClass().add("filter-label");
        classChipPane = new TilePane(); classChipPane.setHgap(8); classChipPane.setVgap(8); classChipPane.setPrefColumns(6); classChipPane.getStyleClass().add("chip-pane");
        Label lblStudent = new Label("Öğrenci:"); lblStudent.getStyleClass().add("filter-label");
        studentSelector = new ComboBox<>(); studentSelector.setConverter(getUserStringConverter()); studentSelector.getStyleClass().add("filter-combo");

        Button clearFilter = new Button("X"); clearFilter.getStyleClass().add("filter-clear-btn"); clearFilter.setOnAction(e -> {
            selectedClassFilter = null;
            studentSelector.setValue(null);
            // reset chip styles
            for (javafx.scene.Node n : classChipPane.getChildren()) if (n instanceof Button) n.setStyle("");
            refreshAllData();
        });

        filterBox.getChildren().addAll(lblClass, classChipPane, lblStudent, studentSelector, clearFilter);

        taskTable = new TableView<>(); taskTable.setPlaceholder(new Label("Görev yok."));
        TableColumn<Task, String> studentCol = new TableColumn<>("ÖĞRENCİ"); studentCol.setPrefWidth(180); studentCol.setCellValueFactory(d -> {
            User k = userService.getUserById(d.getValue().getKidId()); return new SimpleStringProperty(k != null ? formatText(k.getName()) : "Bilinmiyor");
        });
        TableColumn<Task, String> nameCol = new TableColumn<>("GÖREV"); nameCol.setPrefWidth(300); nameCol.setCellValueFactory(d -> new SimpleStringProperty(formatText(d.getValue().getTaskName())));
        TableColumn<Task, Integer> ptsCol = new TableColumn<>("PUAN"); ptsCol.setPrefWidth(80); ptsCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPoints())); ptsCol.setStyle("-fx-alignment:CENTER;");

        taskTable.getColumns().addAll(studentCol, nameCol, ptsCol);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TitledPaneFormBuilder addForm = new TitledPaneFormBuilder(); // helper defined below
        VBox form = addForm.build();

        v.getChildren().addAll(filterBox, taskTable, form);
        VBox.setVgrow(taskTable, Priority.ALWAYS);
        return v;
    }

    // Small helper builder to keep code tidy (no extra file)
    private class TitledPaneFormBuilder {
        VBox build() {
            GridPane grid = new GridPane(); grid.setHgap(20); grid.setVgap(15); grid.setPadding(new Insets(20)); grid.getStyleClass().add("form-card");
            Label lblTask = new Label("GÖREV ADI"); lblTask.getStyleClass().add("input-label");
            TextField taskName = new TextField(); taskName.setPromptText("Örn: Ödevleri kontrol et"); taskName.getStyleClass().add("modern-input");
            Label lblPoints = new Label("PUAN"); lblPoints.getStyleClass().add("input-label"); TextField pointsField = new TextField(); pointsField.setPromptText("50"); pointsField.getStyleClass().add("modern-input");
            Label lblDate = new Label("SON TARİH"); lblDate.getStyleClass().add("input-label"); DatePicker datePicker = new DatePicker(LocalDate.now()); datePicker.getStyleClass().add("modern-input");

            Button assignBtn = new Button("Görevi Ata"); assignBtn.getStyleClass().add("button-primary"); assignBtn.setMaxWidth(Double.MAX_VALUE);

            grid.add(lblTask, 0, 0); grid.add(taskName, 0, 1);
            grid.add(lblPoints, 1, 0); grid.add(pointsField, 1, 1);
            grid.add(lblDate, 0, 2); grid.add(datePicker, 0, 3);
            grid.add(assignBtn, 0, 4, 2, 1);

            assignBtn.setOnAction(e -> {
                String name = taskName.getText().trim(); if (name.isEmpty() || pointsField.getText().trim().isEmpty()) { showCustomDialog("ERROR", "Eksik Bilgi", "Görev adı ve puan gerekli."); return; }
                int p = 0; try { p = Integer.parseInt(pointsField.getText().trim()); } catch (NumberFormatException ex) { showCustomDialog("ERROR", "Hatalı Puan", "Puan sayısal olmalı."); return; }

                // Eğer öğrenci seçiliyse tekine, değilse sınıfa ata
                User selectedStudent = studentSelector.getValue();
                String selectedClass = selectedClassFilter;
                if (selectedStudent != null) {
                    taskService.addTask(selectedStudent.getId(), name, "", p, datePicker.getValue(), "Class", currentTeacher.getName());
                } else if (selectedClass != null) {
                    List<User> kids = userService.getKidsOfClass(selectedClass);
                    for (User k : kids) taskService.addTask(k.getId(), name, "", p, datePicker.getValue(), "Class", currentTeacher.getName());
                } else {
                    showCustomDialog("ERROR", "Hedef Belirle", "Lütfen bir sınıf veya öğrenci seçin."); return;
                }
                taskName.clear(); pointsField.clear(); refreshAllData(); showCustomDialog("SUCCESS", "Görev Atandı", "Görev başarılı şekilde atandı.");
            });

            VBox box = new VBox(12); box.getChildren().addAll(grid); return box;
        }
    }

    private VBox createKidCard(User kid) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMinWidth(250);
        card.setAlignment(Pos.CENTER);

        Label nameLbl = new Label(formatText(kid.getName()) + " " + formatText(kid.getSurname()));
        nameLbl.getStyleClass().add("card-title");

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

    // selection via chips — onClassSelected removed

    private void refreshAllData() {
        // classes
        classesPane.getChildren().clear();
        List<String> classes = currentTeacher.getClassIds() == null ? List.of() : currentTeacher.getClassIds();
        for (String c : classes) {
            int count = userService.getKidsOfClass(c).size();
            VBox card = new VBox(8); card.getStyleClass().add("card"); card.setMinWidth(220); card.setAlignment(Pos.CENTER);
            Label title = new Label(c); title.getStyleClass().add("card-title"); Label info = new Label(count + " öğrenci"); info.getStyleClass().add("card-level");
            card.getChildren().addAll(title, new Separator(), info);
            // make card look clickable and add hover effect
            card.setStyle("-fx-cursor: hand; -fx-padding: 12; -fx-background-radius: 12;");
            DropShadow hover = new DropShadow(); hover.setRadius(8); hover.setOffsetY(2); hover.setColor(Color.rgb(0,0,0,0.18));
            card.setOnMouseEntered(ev -> { card.setEffect(hover); card.setScaleX(1.02); card.setScaleY(1.02); });
            card.setOnMouseExited(ev -> { card.setEffect(null); card.setScaleX(1); card.setScaleY(1); });
            javafx.scene.control.Tooltip.install(card, new javafx.scene.control.Tooltip("Sınıfa tıklayarak öğrencileri görüntüle"));

            // clicking a class shows students of that class
            card.setOnMouseClicked(ev -> {
                List<User> kids = userService.getKidsOfClass(c);
                studentsPane.getChildren().clear();
                for (User k : kids) studentsPane.getChildren().add(createKidCard(k));
                // set title and toggle visibility
                ((Label)((HBox)studentsContainer.getChildren().get(0)).getChildren().get(0)).setText("Sınıf: " + c + " - Öğrenciler");
                classesPane.setVisible(false); classesPane.setManaged(false);
                studentsContainer.setVisible(true); studentsContainer.setManaged(true);
            });
            classesPane.getChildren().add(card);
        }

        // populate class chips
        classChipPane.getChildren().clear();
        for (String cls : classes) {
            Button chip = new Button(cls);
            chip.getStyleClass().add("chip");
            chip.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 6 10; -fx-background-radius: 12;");
            if (cls.equals(selectedClassFilter)) {
                chip.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;");
            }
            chip.setOnAction(ev -> {
                selectedClassFilter = cls;
                // update student selector and tasks
                List<User> kids = userService.getKidsOfClass(cls);
                studentSelector.setItems(FXCollections.observableArrayList(kids));
                // update chip styles
                for (javafx.scene.Node n : classChipPane.getChildren()) if (n instanceof Button) n.setStyle("");
                chip.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 12;");
                refreshAllData();
            });
            classChipPane.getChildren().add(chip);
        }

        // populate tasks
        List<Task> tasks = taskService.getAllTasks().stream()
                .filter(t -> {
                    // show tasks that teacher assigned (by name) or tasks for classes teacher manages
                    if (currentTeacher.getName().equals(t.getAssignedBy())) return true;
                    String kidClass = userService.getUserById(t.getKidId()) != null ? userService.getUserById(t.getKidId()).getClassId() : null;
                    return kidClass != null && currentTeacher.getClassIds() != null && currentTeacher.getClassIds().contains(kidClass);
                })
                .collect(Collectors.toList());
        taskTable.setItems(FXCollections.observableArrayList(tasks));

        // populate student selector if class chip selected
        if (selectedClassFilter != null) {
            List<User> kids = userService.getKidsOfClass(selectedClassFilter);
            studentSelector.setItems(FXCollections.observableArrayList(kids));
        }

        // --- LEADERBOARD ---
        if (leaderboardPane != null) {
            leaderboardPane.getChildren().clear();
            List<User> usersToShow;
            // CLASS: kids in teacher's classes; GLOBAL: all kids
            String filter = (leaderboardTitle != null && leaderboardTitle.getText() != null && leaderboardTitle.getText().contains("Global")) ? "GLOBAL" : "CLASS";
            if ("GLOBAL".equals(filter)) usersToShow = userService.getAllKids();
            else {
                java.util.List<User> agg = new java.util.ArrayList<>();
                if (currentTeacher.getClassIds() != null) for (String c : currentTeacher.getClassIds()) agg.addAll(userService.getKidsOfClass(c));
                usersToShow = agg;
            }
            usersToShow.sort(java.util.Comparator.comparingInt(User::getTotalPoints).reversed());
            int rank = 1; for (User u : usersToShow) { leaderboardPane.getChildren().add(createLeaderCard(u, rank++)); }
        }
    }

    private StringConverter<User> getUserStringConverter() {
        return new StringConverter<>() {
            @Override public String toString(User user) { return user == null ? null : user.getName(); }
            @Override public User fromString(String string) { return null; }
        };
    }

    // --- TAB: LIDERLIK ---
    private VBox createLeaderboardTabContent() {
        VBox v = new VBox(20); v.setPadding(new Insets(20));
        HBox filterBox = new HBox(15); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.getStyleClass().add("filter-pane");
        Label lblFilter = new Label("SIRALAMA:"); lblFilter.getStyleClass().add("filter-label");
        ToggleButton btnClass = new ToggleButton("Sınıfım 🏫"); ToggleButton btnGlobal = new ToggleButton("Global (Dünya) 🌍");
        ToggleGroup g = new ToggleGroup(); btnClass.setToggleGroup(g); btnGlobal.setToggleGroup(g); btnClass.setSelected(true);
        btnClass.getStyleClass().add("filter-toggle"); btnGlobal.getStyleClass().add("filter-toggle");
        filterBox.getChildren().addAll(lblFilter, btnClass, btnGlobal);

        leaderboardTitle = new Label("🏫 Sınıfım Sıralaması"); leaderboardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f1c40f;");
        leaderboardPane = new TilePane(); leaderboardPane.setHgap(20); leaderboardPane.setVgap(20); leaderboardPane.setPrefColumns(3);
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(leaderboardPane);
        scroller.setFitToWidth(true); scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER); scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.getStyleClass().add("leader-scroll"); VBox.setVgrow(scroller, Priority.ALWAYS);

        btnClass.setOnAction(e -> { if (btnClass.isSelected()) { leaderboardTitle.setText("🏫 Sınıfım Sıralaması"); refreshAllData(); }});
        btnGlobal.setOnAction(e -> { if (btnGlobal.isSelected()) { leaderboardTitle.setText("🌍 Global Sıralama"); refreshAllData(); }});

        v.getChildren().addAll(filterBox, leaderboardTitle, scroller);
        return v;
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

    private void showCustomDialog(String type, String title, String message) {
        Stage dialog = new Stage(); dialog.initModality(Modality.APPLICATION_MODAL); dialog.initStyle(StageStyle.TRANSPARENT);
        VBox card = new VBox(15); card.setAlignment(Pos.CENTER); card.getStyleClass().add("custom-dialog");
        String icon = "ⓘ"; String btnClass = "dialog-btn";
        if ("SUCCESS".equals(type)) icon = "✅"; else if ("ERROR".equals(type)) { icon = "⚠"; btnClass = "dialog-btn-error"; }
        Label iconLbl = new Label(icon); iconLbl.getStyleClass().add("dialog-icon"); Label titleLbl = new Label(title); titleLbl.getStyleClass().add("dialog-title");
        Label msgLbl = new Label(message); msgLbl.getStyleClass().add("dialog-message"); msgLbl.setWrapText(true);
        Button closeBtn = new Button("Tamam"); closeBtn.getStyleClass().add("dialog-btn"); if (!"dialog-btn".equals(btnClass)) closeBtn.getStyleClass().add(btnClass); closeBtn.setOnAction(e -> dialog.close());
        card.getChildren().addAll(iconLbl, titleLbl, msgLbl, closeBtn);
        StackPane root = new StackPane(card); root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) {}
        dialog.setScene(scene); dialog.showAndWait();
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder(); java.util.Locale trLocale = java.util.Locale.forLanguageTag("tr");
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
