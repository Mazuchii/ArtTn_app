package edu.Projet_java.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private String url="jdbc:mysql://localhost:3306/esprit_museum";
    private String login="root";
    private String pwd="";

    private Connection cnx;
    public static MyConnection instance;

    private MyConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx = DriverManager.getConnection(url,login,pwd);
            System.out.println("Connexion établie!");

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            System.err.println("Add mysql-connector-java jar to classpath or Maven dependency.");
            cnx = null;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.err.println("Check MySQL running, DB exists, credentials correct.");
            cnx = null;
        }
    }

    public Connection getCnx() {
        return cnx;
    }

    public static MyConnection getInstance(){
        if(instance == null){
            instance = new MyConnection();
        }
        return instance;
    }
}
