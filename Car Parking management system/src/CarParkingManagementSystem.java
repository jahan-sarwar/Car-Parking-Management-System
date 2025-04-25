import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

public class CarParkingManagementSystem {
    // Constants
    private static final int MAX_PARKING_SLOTS = 100;
    private static final double HOURLY_RATE = 2.50;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    // Database connection
    private static Connection connection;

    // Current user
    private static User currentUser = null;

    // Scanner for user input
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        initializeDatabaseConnection();
        initializeSystem();
        showWelcomeScreen();
        closeDatabaseConnection();
    }

    private static void initializeDatabaseConnection() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Database connection parameters
            String url = "jdbc:mysql://localhost:3306/car_parking_db";
            String username = "parking_admin";
            String password = "password123";

            // Establish connection
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to database successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    private static void initializeSystem() {
        // Create tables if they don't exist
        createTables();

        // Initialize parking slots if they don't exist
        initializeParkingSlots();
    }

    private static void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create users table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL)");

            // Create parking_slots table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS parking_slots (" +
                    "slot_id VARCHAR(10) PRIMARY KEY, " +
                    "available BOOLEAN NOT NULL, " +
                    "license_plate VARCHAR(20), " +
                    "owner_name VARCHAR(100), " +
                    "is_registered BOOLEAN, " +
                    "entry_time DATETIME)");

            // Create registered_vehicles table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS registered_vehicles (" +
                    "license_plate VARCHAR(20) PRIMARY KEY, " +
                    "owner_name VARCHAR(100) NOT NULL, " +
                    "vehicle_type VARCHAR(50), " +
                    "contact_no VARCHAR(20))");

            // Create transactions table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id VARCHAR(20) PRIMARY KEY, " +
                    "license_plate VARCHAR(20) NOT NULL, " +
                    "slot_id VARCHAR(10) NOT NULL, " +
                    "entry_time DATETIME NOT NULL, " +
                    "exit_time DATETIME, " +
                    "hours_parked DOUBLE, " +
                    "amount_paid DOUBLE)");

            // Insert admin user if not exists
            if (!userExists(ADMIN_USERNAME)) {
                stmt.executeUpdate("INSERT INTO users VALUES ('" + ADMIN_USERNAME + "', '" +
                        ADMIN_PASSWORD + "', 'ADMIN')");
            }

        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    private static boolean userExists(String username) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT username FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private static void initializeParkingSlots() {
        try (Statement stmt = connection.createStatement()) {
            // Check if slots already exist
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM parking_slots");
            rs.next();
            int count = rs.getInt(1);

            if (count == 0) {
                // Initialize parking slots
                for (int i = 1; i <= MAX_PARKING_SLOTS; i++) {
                    String slotId = "SL" + String.format("%03d", i);
                    stmt.executeUpdate("INSERT INTO parking_slots (slot_id, available) VALUES ('" +
                            slotId + "', true)");
                }
                System.out.println("Initialized " + MAX_PARKING_SLOTS + " parking slots.");
            }
        } catch (SQLException e) {
            System.err.println("Error initializing parking slots: " + e.getMessage());
        }
    }

    private static void showWelcomeScreen() {
        while (true) {
            System.out.println("\n===== CAR PARKING MANAGEMENT SYSTEM =====");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        System.out.println("Thank you for using the system. Goodbye!");
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static void login() {
        System.out.println("\n----- LOGIN -----");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
            currentUser = new User(ADMIN_USERNAME, ADMIN_PASSWORD, "ADMIN");
            showAdminMenu();
        } else {
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    currentUser = new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"));

                    if (currentUser.getRole().equals("STAFF")) {
                        showStaffMenu();
                    } else {
                        System.out.println("Unknown user role.");
                    }
                } else {
                    System.out.println("Invalid username or password.");
                }
            } catch (SQLException e) {
                System.err.println("Database error during login: " + e.getMessage());
            }
        }
    }

    private static void showAdminMenu() {
        while (true) {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. View Parking Status");
            System.out.println("2. Add Staff User");
            System.out.println("3. Remove Staff User");
            System.out.println("4. View All Staff");
            System.out.println("5. View All Transactions");
            System.out.println("6. Generate Report");
            System.out.println("7. View Registered Vehicles");
            System.out.println("8. Logout");
            System.out.print("Enter your choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        viewParkingStatus();
                        break;
                    case 2:
                        addStaffUser();
                        break;
                    case 3:
                        removeStaffUser();
                        break;
                    case 4:
                        viewAllStaff();
                        break;
                    case 5:
                        viewAllTransactions();
                        break;
                    case 6:
                        generateReport();
                        break;
                    case 7:
                        viewRegisteredVehicles();
                        break;
                    case 8:
                        currentUser = null;
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static void showStaffMenu() {
        while (true) {
            System.out.println("\n===== STAFF MENU =====");
            System.out.println("1. Park Vehicle");
            System.out.println("2. Exit Vehicle");
            System.out.println("3. View Available Slots");
            System.out.println("4. View Parking Status");
            System.out.println("5. Register Vehicle");
            System.out.println("6. Logout");
            System.out.print("Enter your choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        parkVehicle();
                        break;
                    case 2:
                        exitVehicle();
                        break;
                    case 3:
                        viewAvailableSlots();
                        break;
                    case 4:
                        viewParkingStatus();
                        break;
                    case 5:
                        registerVehicle();
                        break;
                    case 6:
                        currentUser = null;
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static void viewParkingStatus() {
        System.out.println("\n----- PARKING STATUS -----");
        System.out.printf("%-10s %-15s %-10s %-15s %-20s\n",
                "Slot ID", "Status", "Vehicle", "Owner", "Entry Time");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM parking_slots ORDER BY slot_id")) {

            while (rs.next()) {
                System.out.printf("%-10s %-15s %-10s %-15s %-20s\n",
                        rs.getString("slot_id"),
                        rs.getBoolean("available") ? "Available" : "Occupied",
                        rs.getString("license_plate") != null ? rs.getString("license_plate") : "N/A",
                        rs.getString("owner_name") != null ? rs.getString("owner_name") : "N/A",
                        rs.getTimestamp("entry_time") != null ?
                                rs.getTimestamp("entry_time").toLocalDateTime()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing parking status: " + e.getMessage());
        }
    }

    private static void addStaffUser() {
        System.out.println("\n----- ADD STAFF USER -----");
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT username FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Username already exists.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Database error checking username: " + e.getMessage());
            return;
        }

        System.out.print("Enter password (must be at least 8 characters with 1 uppercase, 1 number and 1 special character): ");
        String password = scanner.nextLine();

        if (!isPasswordValid(password)) {
            System.out.println("Password does not meet security requirements.");
            return;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO users VALUES (?, ?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, "STAFF");
            pstmt.executeUpdate();
            System.out.println("Staff user added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding staff user: " + e.getMessage());
        }
    }

    private static void removeStaffUser() {
        System.out.println("\n----- REMOVE STAFF USER -----");
        System.out.print("Enter username to remove: ");
        String username = scanner.nextLine();

        // Prevent admin from being removed
        if (username.equals(ADMIN_USERNAME)) {
            System.out.println("Cannot remove admin user.");
            return;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT role FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                System.out.println("User not found.");
                return;
            }

            if (!rs.getString("role").equals("STAFF")) {
                System.out.println("Only staff users can be removed.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error checking user: " + e.getMessage());
            return;
        }

        System.out.print("Are you sure you want to remove staff user " + username + "? (Y/N): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("Y")) {
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "DELETE FROM users WHERE username = ?")) {
                pstmt.setString(1, username);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Staff user removed successfully.");
                } else {
                    System.out.println("No user was removed.");
                }
            } catch (SQLException e) {
                System.err.println("Error removing staff user: " + e.getMessage());
            }
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private static void viewAllStaff() {
        System.out.println("\n----- ALL STAFF USERS -----");
        System.out.printf("%-20s\n", "Username");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users WHERE role = 'STAFF' ORDER BY username")) {

            boolean found = false;
            while (rs.next()) {
                System.out.printf("%-20s\n", rs.getString("username"));
                found = true;
            }

            if (!found) {
                System.out.println("No staff users found.");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing staff users: " + e.getMessage());
        }
    }

    private static boolean isPasswordValid(String password) {
        // Password validation: at least 8 chars, 1 uppercase, 1 number, 1 special char
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!@#$%^&*].*");
    }

    private static void viewAllTransactions() {
        System.out.println("\n----- ALL TRANSACTIONS -----");
        System.out.printf("%-10s %-15s %-15s %-20s %-20s %-10s %-10s\n",
                "Trans ID", "License Plate", "Slot ID", "Entry Time", "Exit Time", "Hours", "Amount");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM transactions ORDER BY entry_time DESC")) {

            while (rs.next()) {
                System.out.printf("%-10s %-15s %-15s %-20s %-20s %-10.1f %-10.2f\n",
                        rs.getString("transaction_id"),
                        rs.getString("license_plate"),
                        rs.getString("slot_id"),
                        rs.getTimestamp("entry_time").toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        rs.getTimestamp("exit_time") != null ?
                                rs.getTimestamp("exit_time").toLocalDateTime()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A",
                        rs.getDouble("hours_parked"),
                        rs.getDouble("amount_paid"));
            }
        } catch (SQLException e) {
            System.err.println("Error viewing transactions: " + e.getMessage());
        }
    }

    private static void generateReport() {
        System.out.println("\n----- GENERATE REPORT -----");
        System.out.println("1. Daily Report");
        System.out.println("2. Monthly Report");
        System.out.print("Enter your choice: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    generateDailyReport();
                    break;
                case 2:
                    generateMonthlyReport();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    private static void generateDailyReport() {
        System.out.print("Enter date (yyyy-MM-dd): ");
        String dateStr = scanner.nextLine();

        try {
            LocalDate date = LocalDate.parse(dateStr);
            double totalAmount = 0;
            int totalVehicles = 0;

            System.out.println("\n----- DAILY REPORT FOR " + date + " -----");
            System.out.printf("%-15s %-15s %-10s %-10s\n",
                    "License Plate", "Slot ID", "Hours", "Amount");

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT * FROM transactions WHERE DATE(exit_time) = ?")) {
                pstmt.setDate(1, java.sql.Date.valueOf(date));
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    System.out.printf("%-15s %-15s %-10.1f %-10.2f\n",
                            rs.getString("license_plate"),
                            rs.getString("slot_id"),
                            rs.getDouble("hours_parked"),
                            rs.getDouble("amount_paid"));
                    totalAmount += rs.getDouble("amount_paid");
                    totalVehicles++;
                }
            }

            System.out.println("\nTotal Vehicles: " + totalVehicles);
            System.out.println("Total Revenue: $" + String.format("%.2f", totalAmount));
        } catch (Exception e) {
            System.out.println("Invalid date format or database error: " + e.getMessage());
        }
    }

    private static void generateMonthlyReport() {
        System.out.print("Enter year and month (yyyy-MM): ");
        String monthStr = scanner.nextLine();

        try {
            YearMonth month = YearMonth.parse(monthStr);
            double totalAmount = 0;
            int totalVehicles = 0;

            System.out.println("\n----- MONTHLY REPORT FOR " + month + " -----");
            System.out.printf("%-10s %-15s %-15s %-10s\n",
                    "Date", "Vehicles", "Hours", "Revenue");

            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                double dayAmount = 0;
                int dayVehicles = 0;
                double dayHours = 0;

                try (PreparedStatement pstmt = connection.prepareStatement(
                        "SELECT * FROM transactions WHERE DATE(exit_time) = ?")) {
                    pstmt.setDate(1, java.sql.Date.valueOf(date));
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        dayAmount += rs.getDouble("amount_paid");
                        dayVehicles++;
                        dayHours += rs.getDouble("hours_parked");
                    }
                }

                if (dayVehicles > 0) {
                    System.out.printf("%-10s %-15d %-15.1f %-10.2f\n",
                            date,
                            dayVehicles,
                            dayHours,
                            dayAmount);
                    totalAmount += dayAmount;
                    totalVehicles += dayVehicles;
                }
            }

            System.out.println("\nTotal Vehicles for Month: " + totalVehicles);
            System.out.println("Total Revenue for Month: $" + String.format("%.2f", totalAmount));
        } catch (Exception e) {
            System.out.println("Invalid month format or database error: " + e.getMessage());
        }
    }

    private static void viewRegisteredVehicles() {
        System.out.println("\n----- REGISTERED VEHICLES -----");
        System.out.printf("%-15s %-20s %-15s %-15s\n",
                "License Plate", "Owner Name", "Vehicle Type", "Contact No");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM registered_vehicles ORDER BY license_plate")) {

            while (rs.next()) {
                System.out.printf("%-15s %-20s %-15s %-15s\n",
                        rs.getString("license_plate"),
                        rs.getString("owner_name"),
                        rs.getString("vehicle_type"),
                        rs.getString("contact_no"));
            }
        } catch (SQLException e) {
            System.err.println("Error viewing registered vehicles: " + e.getMessage());
        }
    }

    private static void parkVehicle() {
        System.out.println("\n----- PARK VEHICLE -----");

        // Check for available slots
        if (getAvailableSlotsCount() == 0) {
            System.out.println("Sorry, parking is full.");
            return;
        }

        System.out.print("Enter vehicle license plate: ");
        String licensePlate = scanner.nextLine().toUpperCase();

        // Check if vehicle is already parked
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT slot_id FROM parking_slots WHERE license_plate = ? AND available = false")) {
            pstmt.setString(1, licensePlate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("This vehicle is already parked in slot " + rs.getString("slot_id"));
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error checking parked vehicles: " + e.getMessage());
            return;
        }

        // Check if vehicle is registered
        boolean isRegistered = false;
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT license_plate FROM registered_vehicles WHERE license_plate = ?")) {
            pstmt.setString(1, licensePlate);
            ResultSet rs = pstmt.executeQuery();
            isRegistered = rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking vehicle registration: " + e.getMessage());
            return;
        }

        if (!isRegistered) {
            System.out.println("Vehicle is not registered. Would you like to register it now? (Y/N)");
            String choice = scanner.nextLine();
            if (choice.equalsIgnoreCase("Y")) {
                registerVehicle(licensePlate);
                isRegistered = true;
            }
        }

        // Find first available slot
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT slot_id FROM parking_slots WHERE available = true LIMIT 1")) {
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String slotId = rs.getString("slot_id");
                System.out.print("Enter owner name: ");
                String ownerName = scanner.nextLine();

                // Park the vehicle
                try (PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE parking_slots SET available = false, license_plate = ?, " +
                                "owner_name = ?, is_registered = ?, entry_time = ? WHERE slot_id = ?")) {
                    updateStmt.setString(1, licensePlate);
                    updateStmt.setString(2, ownerName);
                    updateStmt.setBoolean(3, isRegistered);
                    updateStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    updateStmt.setString(5, slotId);
                    updateStmt.executeUpdate();
                }

                // Create transaction
                try (PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO transactions (transaction_id, license_plate, slot_id, entry_time) " +
                                "VALUES (?, ?, ?, ?)")) {
                    insertStmt.setString(1, "TXN" + System.currentTimeMillis());
                    insertStmt.setString(2, licensePlate);
                    insertStmt.setString(3, slotId);
                    insertStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();
                }

                System.out.println("Vehicle parked successfully in slot " + slotId);
                System.out.println("Entry time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else {
                System.out.println("No available slots found.");
            }
        } catch (SQLException e) {
            System.err.println("Error parking vehicle: " + e.getMessage());
        }
    }

    private static void exitVehicle() {
        System.out.println("\n----- EXIT VEHICLE -----");
        System.out.print("Enter vehicle license plate: ");
        String licensePlate = scanner.nextLine().toUpperCase();

        try {
            // Get parking details
            String slotId = null;
            LocalDateTime entryTime = null;

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT slot_id, entry_time FROM parking_slots WHERE license_plate = ? AND available = false")) {
                pstmt.setString(1, licensePlate);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    slotId = rs.getString("slot_id");
                    entryTime = rs.getTimestamp("entry_time").toLocalDateTime();
                } else {
                    System.out.println("Vehicle not found in parking.");
                    return;
                }
            }

            LocalDateTime exitTime = LocalDateTime.now();
            long hoursParked = Duration.between(entryTime, exitTime).toHours();
            if (Duration.between(entryTime, exitTime).toMinutes() % 60 > 0) {
                hoursParked++; // Round up to the next hour
            }
            double amountDue = hoursParked * HOURLY_RATE;

            System.out.println("\n----- PARKING RECEIPT -----");
            System.out.println("License Plate: " + licensePlate);
            System.out.println("Slot ID: " + slotId);
            System.out.println("Entry Time: " + entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("Exit Time: " + exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("Hours Parked: " + hoursParked);
            System.out.println("Amount Due: $" + String.format("%.2f", amountDue));

            System.out.print("\nConfirm payment received? (Y/N): ");
            String confirm = scanner.nextLine();

            if (confirm.equalsIgnoreCase("Y")) {
                // Update the transaction
                try (PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE transactions SET exit_time = ?, hours_parked = ?, amount_paid = ? " +
                                "WHERE license_plate = ? AND slot_id = ? AND exit_time IS NULL")) {
                    pstmt.setTimestamp(1, Timestamp.valueOf(exitTime));
                    pstmt.setDouble(2, hoursParked);
                    pstmt.setDouble(3, amountDue);
                    pstmt.setString(4, licensePlate);
                    pstmt.setString(5, slotId);
                    pstmt.executeUpdate();
                }

                // Release the parking slot
                try (PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE parking_slots SET available = true, license_plate = NULL, " +
                                "owner_name = NULL, is_registered = NULL, entry_time = NULL WHERE slot_id = ?")) {
                    pstmt.setString(1, slotId);
                    pstmt.executeUpdate();
                }

                System.out.println("Payment confirmed. Thank you!");
            } else {
                System.out.println("Payment not confirmed. Vehicle remains parked.");
            }
        } catch (SQLException e) {
            System.err.println("Error during vehicle exit: " + e.getMessage());
        }
    }

    private static void viewAvailableSlots() {
        System.out.println("\n----- AVAILABLE PARKING SLOTS -----");
        int count = 0;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT slot_id FROM parking_slots WHERE available = true ORDER BY slot_id")) {

            while (rs.next()) {
                System.out.print(rs.getString("slot_id") + " ");
                count++;
                if (count % 10 == 0) System.out.println();
            }

            if (count == 0) {
                System.out.println("No available slots.");
            } else {
                System.out.println("\nTotal available slots: " + count);
            }
        } catch (SQLException e) {
            System.err.println("Error viewing available slots: " + e.getMessage());
        }
    }

    private static int getAvailableSlotsCount() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM parking_slots WHERE available = true")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting available slots: " + e.getMessage());
            return 0;
        }
    }

    private static void registerVehicle() {
        System.out.println("\n----- REGISTER VEHICLE -----");
        System.out.print("Enter vehicle license plate: ");
        String licensePlate = scanner.nextLine().toUpperCase();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT license_plate FROM registered_vehicles WHERE license_plate = ?")) {
            pstmt.setString(1, licensePlate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Vehicle is already registered.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error checking vehicle registration: " + e.getMessage());
            return;
        }

        System.out.print("Enter owner name: ");
        String ownerName = scanner.nextLine();

        System.out.print("Enter vehicle type: ");
        String vehicleType = scanner.nextLine();

        System.out.print("Enter contact number: ");
        String contactNo = scanner.nextLine();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO registered_vehicles VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, licensePlate);
            pstmt.setString(2, ownerName);
            pstmt.setString(3, vehicleType);
            pstmt.setString(4, contactNo);
            pstmt.executeUpdate();
            System.out.println("Vehicle registered successfully.");
        } catch (SQLException e) {
            System.err.println("Error registering vehicle: " + e.getMessage());
        }
    }

    private static void registerVehicle(String licensePlate) {
        System.out.println("\n----- REGISTER VEHICLE -----");
        System.out.println("License Plate: " + licensePlate);

        System.out.print("Enter owner name: ");
        String ownerName = scanner.nextLine();

        System.out.print("Enter vehicle type: ");
        String vehicleType = scanner.nextLine();

        System.out.print("Enter contact number: ");
        String contactNo = scanner.nextLine();

        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO registered_vehicles VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, licensePlate);
            pstmt.setString(2, ownerName);
            pstmt.setString(3, vehicleType);
            pstmt.setString(4, contactNo);
            pstmt.executeUpdate();
            System.out.println("Vehicle registered successfully.");
        } catch (SQLException e) {
            System.err.println("Error registering vehicle: " + e.getMessage());
        }
    }
}

class User {
    private String username;
    private String password;
    private String role;

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}