package project.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import project.model.User;
import project.service.UserService;

public class LoginView {

    private final UserService userService = new UserService();
    
    // ParentDashboard'daki gibi ana sahneyi burada tutuyoruz
    private Stage primaryStage;

    public void show(Stage stage) {
        this.primaryStage = stage; // Ana sahneyi kaydet

        StackPane root = new StackPane();
        root.getStyleClass().add("login-bg");

        // --- GİRİŞ KARTI ---
        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");

        // 1. Logo (use /logo.png from resources when available)
        ImageView logoView = null;
        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/logo.png"));
            if (logoImg != null) {
                logoView = new ImageView(logoImg);
                logoView.setFitHeight(80);
                logoView.setPreserveRatio(true);
            }
        } catch (Exception ex) {
            logoView = null;
        }

        Label titleLbl = new Label("KidTask");
        titleLbl.getStyleClass().add("login-title");
        Label subTitleLbl = new Label("Aile ve Okul Görev Platformu");
        subTitleLbl.getStyleClass().add("login-subtitle");

        // 2. Form
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER_LEFT);

        Label lblUser = new Label("KULLANICI ADI"); lblUser.getStyleClass().add("input-label");
        TextField userField = new TextField(); userField.getStyleClass().add("modern-input");

        Label lblPass = new Label("ŞİFRE"); lblPass.getStyleClass().add("input-label");
        PasswordField passField = new PasswordField(); passField.getStyleClass().add("modern-input");

        passField.setOnAction(e -> handleLogin(stage, userField.getText(), passField.getText()));

        formBox.getChildren().addAll(lblUser, userField, lblPass, passField);

        // 3. Butonlar
        Button loginBtn = new Button("Giriş Yap 🔐");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setStyle("-fx-font-size: 16px; -fx-padding: 12 20;");
        loginBtn.setOnAction(e -> handleLogin(stage, userField.getText(), passField.getText()));

        // KAYIT OL BUTONU
        Button registerLink = new Button("Hesabın yok mu? Kayıt Ol ✨");
        registerLink.getStyleClass().add("link-button");
        registerLink.setOnAction(e -> showRoleSelectionDialog());

        if (logoView != null) card.getChildren().add(logoView);
        else {
            Label logoEmoji = new Label("🚀");
            logoEmoji.setStyle("-fx-font-size: 60px;");
            card.getChildren().add(logoEmoji);
        }
        card.getChildren().addAll(titleLbl, subTitleLbl, formBox, loginBtn, registerLink);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 1100, 750);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ex) {}

        stage.setTitle("KidTask - Giriş");
        stage.setScene(scene);
        stage.show();
    }

    private void handleLogin(Stage stage, String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showCustomDialog("ERROR", "Hata", "Lütfen bilgileri giriniz.");
            return;
        }
        User user = userService.login(username, password);
        if (user != null) {
            stage.close();
            if ("parent".equalsIgnoreCase(user.getRole())) new ParentDashboardView(user).show();
            else if ("kid".equalsIgnoreCase(user.getRole())) new KidDashboardView(user).show();
            else if ("teacher".equalsIgnoreCase(user.getRole())) new TeacherDashboardView(user).show();
            else showCustomDialog("SUCCESS", "Giriş Başarılı", "Giriş başarılı.");
        } else {
            showCustomDialog("ERROR", "Hata", "Kullanıcı adı veya şifre yanlış.");
        }
    }

    // =========================================================================
    //   ROL SEÇİM EKRANI (ParentDashboard Mantığıyla)
    // =========================================================================
    private void showRoleSelectionDialog() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT); // Şeffaf (Modern)

        // KRİTİK: Sahibini ana pencere yapıyoruz ve window-modal kullanıyoruz
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("custom-dialog");
        card.setMinWidth(450); 

        Label title = new Label("KİMSİN? 🤔");
        title.getStyleClass().add("dialog-title");
        Label sub = new Label("Kayıt olmak için rolünü seç.");
        sub.getStyleClass().add("dialog-message");

        Button btnKid = createRoleButton("👶 Öğrenci", "button-secondary");
        Button btnParent = createRoleButton("👪 Ebeveyn", "button-primary");
        Button btnTeacher = createRoleButton("🎓 Öğretmen", "button-refresh");

        btnKid.setOnAction(e -> showRegisterForm(dialog, "kid"));
        btnParent.setOnAction(e -> showRegisterForm(dialog, "parent"));
        btnTeacher.setOnAction(e -> showRegisterForm(dialog, "teacher"));

        Button closeBtn = new Button("Vazgeç");
        closeBtn.getStyleClass().add("link-button");
        closeBtn.setOnAction(e -> dialog.close());

        card.getChildren().addAll(title, sub, btnKid, btnParent, btnTeacher, closeBtn);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) {}

        dialog.setScene(scene);
        dialog.show();
    }

    private Button createRoleButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-font-size: 16px; -fx-padding: 15;");
        return btn;
    }

    // =========================================================================
    //   KAYIT FORMU (Aynı Pencere İçinde Değişim)
    // =========================================================================
    private void showRegisterForm(Stage dialog, String role) {
        // Mevcut pencerenin (dialog) içeriğini alıp temizliyoruz
        StackPane root = (StackPane) dialog.getScene().getRoot();
        VBox card = (VBox) root.getChildren().get(0);
        card.getChildren().clear();

        String roleTitle = switch (role) {
            case "kid" -> "Öğrenci Kaydı 🎒";
            case "parent" -> "Ebeveyn Kaydı 👪";
            case "teacher" -> "Öğretmen Kaydı 🎓";
            default -> "Kayıt";
        };

        Label title = new Label(roleTitle);
        title.getStyleClass().add("dialog-title");

        // Modern inner card
        VBox innerCard = new VBox(12);
        innerCard.setPadding(new Insets(18));
        innerCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        DropShadow ds = new DropShadow(); ds.setRadius(10); ds.setOffsetY(3); ds.setColor(Color.rgb(0,0,0,0.18));
        innerCard.setEffect(ds);
        innerCard.setMaxWidth(520);

        TextField nameField = new TextField(); nameField.setPromptText("Ad"); nameField.getStyleClass().add("modern-input");
        TextField surnameField = new TextField(); surnameField.setPromptText("Soyad"); surnameField.getStyleClass().add("modern-input");
        TextField usernameField = new TextField(); usernameField.setPromptText("Kullanıcı Adı (Örn: beyza)"); usernameField.getStyleClass().add("modern-input");
        TextField passField = new PasswordField(); passField.setPromptText("Şifre Belirle"); passField.getStyleClass().add("modern-input");

        TextField ageField = new TextField(); ageField.setPromptText("Yaş (Örn: 10)"); ageField.getStyleClass().add("modern-input");
        TextField classIdField = new TextField(); classIdField.setPromptText("Sınıf Kodu (Opsiyonel)"); classIdField.getStyleClass().add("modern-input");

        // For teachers we allow multiple class IDs shown as removable "chips"
        FlowPane chips = new FlowPane();
        chips.setHgap(8);
        chips.setVgap(8);
        chips.setPrefWrapLength(480);

        TextField classInput = new TextField();
        classInput.setPromptText("Sınıf Kodu (Örn: 3A)");
        classInput.getStyleClass().add("modern-input");

        Button addClassBtn = new Button("+ Yeni Sınıf Ekle");
        addClassBtn.getStyleClass().add("link-button");
        addClassBtn.setOnAction(ev -> {
            String v = classInput.getText().trim();
            if (v.isEmpty()) return;
            Label lbl = new Label(v);
            lbl.getStyleClass().add("class-chip-label");
            Button remove = new Button("✖");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            HBox chip = new HBox(8, lbl, remove);
            chip.getStyleClass().add("class-chip");
            chip.setAlignment(Pos.CENTER);
            remove.setOnAction(e2 -> chips.getChildren().remove(chip));
            chips.getChildren().add(chip);
            classInput.clear();
        });

        VBox fieldsBox = new VBox(12);
        fieldsBox.getChildren().addAll(nameField, surnameField, usernameField, passField);

        if ("kid".equals(role)) {
            fieldsBox.getChildren().addAll(ageField, classIdField);
        }
        if ("teacher".equals(role)) {
            // show chips container and input for teachers
            HBox classInputRow = new HBox(8, classInput, addClassBtn);
            classInputRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(classInput, Priority.ALWAYS);
            fieldsBox.getChildren().addAll(classInputRow, chips);
        }

        // Wrap fieldsBox in a ScrollPane so many class inputs can scroll
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(fieldsBox);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setPrefViewportHeight(300);

        Button saveBtn = new Button("Kaydı Tamamla ✅");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = new Button("Geri Dön");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> { dialog.close(); showRoleSelectionDialog(); });

        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || passField.getText().isEmpty() || usernameField.getText().isEmpty()) {
                showCustomDialog("ERROR", "Eksik Bilgi", "Ad, kullanıcı adı ve şifre zorunludur.");
                return;
            }

            // username uniqueness
            String desiredUsername = usernameField.getText().trim();
            if (userService.getUserByUsername(desiredUsername) != null) {
                showCustomDialog("ERROR", "Kullanıcı Adı Kullanılıyor", "Bu kullanıcı adı alınmış. Başka bir kullanıcı adı deneyin.");
                return;
            }

            User newUser;
            if ("parent".equals(role)) {
                newUser = User.createParent(nameField.getText(), surnameField.getText(), passField.getText());
                newUser.setUsername(desiredUsername);
            } else if ("teacher".equals(role)) {
                newUser = User.createTeacher(nameField.getText(), surnameField.getText(), passField.getText());
                newUser.setUsername(desiredUsername);
                // collect class ids from chips FlowPane
                java.util.List<String> classIds = new java.util.ArrayList<>();
                for (javafx.scene.Node node : chips.getChildren()) {
                    if (node instanceof HBox) {
                        HBox hb = (HBox) node;
                        if (!hb.getChildren().isEmpty() && hb.getChildren().get(0) instanceof Label) {
                            String v = ((Label) hb.getChildren().get(0)).getText().trim();
                            if (!v.isEmpty()) classIds.add(v);
                        }
                    }
                }
                if (!classIds.isEmpty()) newUser.setClassIds(classIds);
            } else {
                int age = 0;
                try { if(!ageField.getText().isEmpty()) age = Integer.parseInt(ageField.getText()); } catch(Exception ex){}
                newUser = User.createKid(nameField.getText(), surnameField.getText(), age, classIdField.getText(), passField.getText());
                newUser.setUsername(desiredUsername);
            }

            userService.addUser(newUser);
            // close the registration dialog first, then show a SUCCESS dialog like ParentDashboard
            dialog.close();
            showCustomDialog("SUCCESS", "Kayıt Başarılı", "Hesabınız oluşturuldu. Giriş yapabilirsiniz.");
        });

        // place save/back in a fixed footer so they're always visible
        HBox footer = new HBox(10, backBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 8 0 0 0;");

        // Build inner card content and add to dialog card
        innerCard.getChildren().addAll(title, scroller, footer);
        VBox container = new VBox(innerCard);
        container.setAlignment(Pos.CENTER);
        card.getChildren().add(container);
    }

    // =========================================================================
    //   MODERN POP-UP (ParentDashboard Mantığıyla Birebir Aynı)
    // =========================================================================
    private void showCustomDialog(String type, String title, String message) {
        showCustomDialog(null, type, title, message);
    }

    private void showCustomDialog(Stage owner, String type, String title, String message) {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT); // Şeffaf Stil

        // Owner/modality handling. For SUCCESS popups we avoid modality so the main window isn't blocked
        boolean isSuccess = "SUCCESS".equals(type);
        if (owner != null) {
            dialog.initOwner(owner);
        } else if (primaryStage != null) {
            dialog.initOwner(primaryStage);
        }

        if (isSuccess) {
            dialog.initModality(Modality.NONE);
        } else {
            // Use window-modal when possible to keep user focused, fallback to application-modal
            if (dialog.getOwner() != null) dialog.initModality(Modality.WINDOW_MODAL);
            else dialog.initModality(Modality.APPLICATION_MODAL);
        }

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("custom-dialog");

        String icon = "ⓘ";
        String btnClass = "dialog-btn";
        if ("SUCCESS".equals(type)) { icon = "✅"; }
        else if ("ERROR".equals(type)) { icon = "⚠"; btnClass = "dialog-btn-error"; }

        Label iconLbl = new Label(icon); iconLbl.getStyleClass().add("dialog-icon");
        Label titleLbl = new Label(title); titleLbl.getStyleClass().add("dialog-title");
        Label msgLbl = new Label(message); msgLbl.getStyleClass().add("dialog-message");
        
        Button closeBtn = new Button("Tamam");
        closeBtn.getStyleClass().add("dialog-btn");
        if (!"dialog-btn".equals(btnClass)) closeBtn.getStyleClass().add(btnClass);
        closeBtn.setOnAction(e -> dialog.close());

        card.getChildren().addAll(iconLbl, titleLbl, msgLbl, closeBtn);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ex) {}

        dialog.setScene(scene);
        // Use non-blocking show so UI stays responsive; close via button or auto-close logic if added.
        dialog.show();
    }

    // NOTE: in-window toast removed — dialogs used instead.
}