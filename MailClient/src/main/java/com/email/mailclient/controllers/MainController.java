package com.email.mailclient.controllers;

import com.email.mailclient.model.EmailMessage;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class MainController extends CommonController implements Initializable {


    private String personalEmail = "";
    private String username = "";
    private Properties config;
    private Socket socket;
    static DataInputStream dis;
    static DataOutputStream dos;
    private String loginResponse;
    private String reloadCommand = "SELECT inbox";
    private EmailMessage[] emailMessages;

    private List<EmailMessage> listSelectedEmail = new ArrayList<>();
    EmailMessage selectedEmail;
    @FXML
    private AnchorPane mainComponent;
    @FXML
    private ImageView btnImage;
    @FXML
    private void btnLoadMain(){
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/main.fxml"));
                MainController mainController = new MainController( config, socket, personalEmail, loginResponse);
                loader.setControllerFactory(controllerClass -> mainController);
                Parent root = loader.load();

                // Tạo một Scene với giao diện đã tải
                Scene scene = new Scene(root);

                // Lấy Stage hiện tại
                Stage stage = new Stage();  // Create a new stage for the main screen
                stage.setScene(scene);
                stage.setTitle("Dashboard");

                // Close the login window when switching to the main screen
                Stage loginStage = (Stage) btnImage.getScene().getWindow();
                loginStage.close();

                // Set up any additional configuration for the main stage if needed

                // Show the main screen
                stage.show();

            } catch (IOException e) {
                showError("Lỗi khi tải giao diện trang chủ: " + e.getMessage());

            }
        });
    }

    @FXML
    private VBox listEmailComponent;

    Popup popup;

    @FXML
    private HBox menuButton;

    @FXML
    void reloadListEmail() {
        fetchListEmail(reloadCommand);
        menuButton.getChildren().clear();
    }


    @FXML
    private TableView<EmailMessage> tableViewEmails;
    @FXML
    private TableColumn<EmailMessage, StringProperty> fromColumn;
    @FXML
    private TableColumn<EmailMessage, StringProperty> subjectColumn;
    @FXML
    private TableColumn<EmailMessage, Date> dateColumn;
    @FXML
    private TableColumn<EmailMessage, Boolean> isReadColumn;

    @FXML
    private TextField txtSearchInformation;
    @FXML
    private Label txtUserName;

    @FXML
    void loadReceived() {
        fetchListEmail("SELECT inbox");
        reloadCommand = "SELECT inbox";
        menuButton.getChildren().clear();
    }

    @FXML
    void loadSent() {
        fetchListEmail("SELECT outbox");
        reloadCommand = "SELECT outbox";
        menuButton.getChildren().clear();
    }

    @FXML
    void loadTrash() {
        fetchListEmail("SELECT recycle");
        reloadCommand = "SELECT recycle";
        menuButton.getChildren().clear();
    }


    public MainController(Properties config, Socket socket, String personalEmail, String loginResponse) {
        super();
        this.config = config;
        this.personalEmail = personalEmail;
        this.socket = socket;
        this.loginResponse = loginResponse;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUserName();
        initializeTableColumns();
        fetchListEmail("SELECT inbox");
        // Khởi tạo listSelectedEmail
        listSelectedEmail = new ArrayList<>();
        // Sự kiện click và double click trên tableViewEmails
        tableViewEmails.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                // Xử lý sự kiện click
                selectEmail();
            } else if (event.getClickCount() == 2) {
                // Xử lý sự kiện double click
                loadEmailDetail();
            }
        });



      //
        tableViewEmails.setRowFactory(tv -> {
            TableRow<EmailMessage> row = new TableRow<>();
            row.setOnMouseEntered(event -> {
                if (!row.isEmpty()) {

                }
            });
            row.setOnMouseExited(event -> {


            });
            return row;
        });


        txtUserName.setOnMouseEntered(event -> {

            popup = new Popup();
            Label popupLabel = new Label("This is a Popup!");
            popupLabel.setStyle("-fx-background-color: white; -fx-padding: 10px;");
            popup.getContent().add(popupLabel);
            // Lấy tọa độ của nhãn trên màn hình
            double x = txtUserName.localToScreen(txtUserName.getBoundsInLocal()).getMinX() + 150;
            double y = txtUserName.localToScreen(txtUserName.getBoundsInLocal()).getMinY() + 50;

            // Hiển thị Popup tại vị trí của nhãn
            popup.show(txtUserName, x, y);
        });

        txtUserName.setOnMouseExited(event -> {
            popup.hide();
        });
    }

    private void setUserName() {
        String[] responseParts = loginResponse.split(" ", 4);

        if (responseParts.length >= 4 && "OK".equals(responseParts[0])) {
            String username = responseParts[3];
            this.username = username;
            txtUserName.setText("Chào mừng, " + username);
        }
    }


    private void initializeTableColumns() {
        fromColumn.setCellValueFactory(new PropertyValueFactory<>("sender"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("created_at"));
//        isReadColumn.setCellValueFactory(new PropertyValueFactory<>("isRead"));
        isReadColumn.setCellValueFactory(cellData -> {
            int isReadValue = cellData.getValue().getIsRead();
            return new SimpleBooleanProperty(isReadValue == 1);
        });

        // Optional: Set a cell factory for additional styling
        isReadColumn.setCellFactory(column -> new TableCell<EmailMessage, Boolean>() {
            @Override
            protected void updateItem(Boolean isReadValue, boolean empty) {
                super.updateItem(isReadValue, empty);

                if (empty || isReadValue == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (isReadValue) {
                        setText("Đã đọc");
                        setStyle("-fx-text-fill: green;"); // or any other style
                    } else {
                        setText("Chưa đọc");
                        setStyle("-fx-text-fill: red;"); // or any other style
                    }
                }
            }
        });
    }

    private void selectEmail() {
        // Lấy dòng được chọn
        selectedEmail = tableViewEmails.getSelectionModel().getSelectedItem();

        if (selectedEmail != null) {
            if (listSelectedEmail.contains(selectedEmail)) {
                // Nếu email đã được chọn, hủy chọn và xóa nút
                listSelectedEmail.remove(selectedEmail);
                menuButton.getChildren().clear();
            } else {

                listSelectedEmail.clear();
                menuButton.getChildren().clear();
                // Nếu email chưa được chọn, thêm vào danh sách và tạo nút xóa
                listSelectedEmail.add(selectedEmail);

                if (reloadCommand.equals("SELECT recycle")) {
                    Button btnRestore = new Button("Khôi phục");
                    btnRestore.setOnAction(e -> handleRestoreEmail(selectedEmail.getId()));
                    menuButton.getChildren().add(btnRestore);


                    Button btnDelete = new Button("Xóa");
                    btnDelete.setOnAction(e -> handleDeleteEmail(selectedEmail.getId()));
                    menuButton.getChildren().add(btnDelete);
                } else {

                    Button btnDelete = new Button("Xóa");
                    btnDelete.setOnAction(e -> handleDeleteEmail(selectedEmail.getId()));
                    menuButton.getChildren().add(btnDelete);
                }

            }
        }
    }

    private void handleDeleteEmail(int idEmail) {

        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("DELETE " + idEmail);

            System.out.println("DELETE " + idEmail);
            String Response = dis.readUTF();

            System.out.println(Response);
            if (Response.equals("OK")) {

                // cập nhật lại danh sách email
                fetchListEmail(reloadCommand);
                //hủy chọn
                listSelectedEmail.remove(selectedEmail);
                //xóa nút xóa
                menuButton.getChildren().clear();
            } else {
                System.out.println("Có lỗi xảy ra");
            }
        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }

    }


    private void handleRestoreEmail(int idEmail) {

        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("RESTORE " + idEmail);

            System.out.println("RESTORE " + idEmail);
            String Response = dis.readUTF();

            System.out.println(Response);
            if (Response.equals("OK")) {

                // cập nhật lại danh sách email
                fetchListEmail(reloadCommand);
                //hủy chọn
                listSelectedEmail.remove(selectedEmail);
                //xóa các nút
                menuButton.getChildren().clear();
            } else {
                System.out.println("Có lỗi xảy ra");
            }
        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }

    }




    private void fetchListEmail(String command) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF(command);
            String Response = dis.readUTF();

            String[] responseParts = Response.split(" ", 2);
            if (responseParts.length >= 2) {
                String jsonPart = responseParts[1];
                generateEmailList(jsonPart);
            } else {
                System.out.println("Có lỗi xảy ra");
            }
        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }
    }



    private void generateEmailList(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            showError("Phản hồi từ máy chủ trống hoặc null.");
            return;
        }

        Platform.runLater(() -> {
            try {
                Gson gson = new Gson();

                emailMessages = gson.fromJson(jsonResponse, EmailMessage[].class);

                if (emailMessages != null && emailMessages.length > 0) {
                    ObservableList<EmailMessage> emailList = FXCollections.observableArrayList(emailMessages);

                    if (this.tableViewEmails != null) {
                        this.tableViewEmails.setItems(emailList);
                    } else {
                        System.out.println("Hộp thư trống. (tableViewEmails is null)");
                        // Hoặc hiển thị thông báo lỗi khác tùy thuộc vào yêu cầu của bạn
                    }
                } else {
                    if (this.tableViewEmails != null) {
                        // Đặt tableViewEmails thành một danh sách rỗng khi không có email nào
                        this.tableViewEmails.setItems(FXCollections.emptyObservableList());
                    }
//                    showError("Hộp thư trống!");
                }
            } catch (JsonParseException e) {
                showError("Lỗi khi chuyển đổi dữ liệu JSON: " + e.getMessage());
            }
        });


    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void loadEmailDetail() {
        // Lấy dòng được chọn
        selectedEmail = tableViewEmails.getSelectionModel().getSelectedItem();

        if (selectedEmail != null) {
            System.out.println("Bạn đã chọn email  " + selectedEmail.getId());


            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/email-details.fxml"));
                loader.setControllerFactory(controllerClass -> new EmailDetailsController(config, socket, selectedEmail, loginResponse));
                Parent root = loader.load();

                // Create a new stage
                Stage composeStage = new Stage();
                composeStage.initStyle(StageStyle.DECORATED);
                composeStage.initModality(Modality.APPLICATION_MODAL);
                composeStage.setTitle("Chi Tiết Thư");

                // Set the scene
                Scene scene = new Scene(root);
                composeStage.setScene(scene);

                // Show the stage
                composeStage.show();
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }
        }
    }


    @FXML
    public void loadComposeEmail() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/compose-email.fxml"));
            loader.setControllerFactory(c -> new ComposeEmailController(config, personalEmail));
            Parent root = loader.load();

            // Create a new stage
            Stage composeStage = new Stage();
            composeStage.initStyle(StageStyle.DECORATED);
            composeStage.initModality(Modality.APPLICATION_MODAL);
            composeStage.setTitle("Compose Email");

            // Set the scene
            Scene scene = new Scene(root);
            composeStage.setScene(scene);

            // Show the stage
            composeStage.show();
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }

    }


    @FXML
    private void searchAction() {
        String keyword = txtSearchInformation.getText();

        // Kiểm tra xem có từ khóa tìm kiếm hay không
        if (!keyword.isEmpty()) {
            // Thực hiện tìm kiếm trong danh sách emailMessages
            EmailMessage[] results = searchEmailsByTitle(keyword);

            // Cập nhật giao diện người dùng với kết quả tìm kiếm
            if (results.length > 0) {
                ObservableList<EmailMessage> emailList = FXCollections.observableArrayList(results);

                if (this.tableViewEmails != null) {
                    this.tableViewEmails.setItems(emailList);
                } else {
                    System.out.println("Hộp thư trống.");
                }
            } else {
                showError("Không tìm thấy kết quả cho từ khóa tìm kiếm.");
            }
        } else {
            // Hiển thị thông báo nếu người dùng chưa nhập từ khóa
            showError("Vui lòng nhập từ khóa tìm kiếm.");
        }
    }

    private EmailMessage[] searchEmailsByTitle(String keyword) {
        List<EmailMessage> results = new ArrayList<>();

        // Kiểm tra xem danh sách emailMessages có tồn tại và không rỗng
        if (emailMessages != null && emailMessages.length > 0) {
            // Thực hiện tìm kiếm trong danh sách hiện tại
            for (EmailMessage message : emailMessages) {
                if (message.getSubject().toLowerCase().contains(keyword.toLowerCase())) {
                    results.add(message);
                }
            }
        }

        return results.toArray(new EmailMessage[0]);
    }


    @FXML
    private void loadProfile() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/profile.fxml"));
            loader.setControllerFactory(c -> new ProfileController(config, socket, personalEmail, loginResponse));
            Parent root = loader.load();

            // Create a new stage
            Stage profileStage = new Stage();
            profileStage.initStyle(StageStyle.DECORATED);
            profileStage.initModality(Modality.APPLICATION_MODAL);
            profileStage.setTitle("Tài Khoản");

            // Set the scene
            Scene scene = new Scene(root);
            profileStage.setScene(scene);

            // Show the stage
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }

    }




    @FXML
    private void handleLogout() {
        // Tạo hộp thoại xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất?");
        // Get the user's response when clicking OK or Cancel
        Optional<ButtonType> result = alert.showAndWait();

        // Process logout if the user clicked OK
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Create DataOutputStream and DataInputStream
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Send logout command to the server
                dos.writeUTF("LOGOUT");

                // Receive and print the server's response
                String response = dis.readUTF();
                System.out.println(response);

                // Close the socket
                socket.close();

                // Close the application
//                Platform.exit();
                backToLoginScreen();


            } catch (IOException e) {
                // Handle communication error
                showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
            }
        }


    }

    private void backToLoginScreen() {
        try {
            // Tải tệp FXML cho giao diện đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/login.fxml"));
            loader.setControllerFactory(c -> new LoginController());
            Parent root = loader.load();

            // Tạo một Scene với giao diện đã tải
            Scene scene = new Scene(root);

            // Lấy Stage hiện tại
            Stage stage = new Stage();  // Create a new stage for the main screen
            stage.setScene(scene);
            stage.setTitle("Login");

            // Đặt Scene cho Stage Main và Log
            Stage mainStage = (Stage) btnImage.getScene().getWindow();
            mainStage.close();

            // mở Stage mới
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi tải giao diện đăng nhập: " + e.getMessage());
            // Bạn có thể xử lý lỗi này theo cách cần thiết cho ứng dụng của bạn.
        }
    }

}
