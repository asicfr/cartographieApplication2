package com.carto.apptemoinSimple.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.carto.apptemoinSimple.dao.ClientDao;
import com.carto.apptemoinSimple.model.Client;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

/**
 * @author only2dhir
 *
 */
public class ClientDaoImpl implements ClientDao {
	
	private Connection connection;

	private final String INSERT_SQL = "INSERT INTO CLIENTS(nom,prenom,date_naissance,e_mail) values(?,?,?,?)";
	private final String FETCH_SQL = "select id, nom, prenom, date_naissance, e_mail from clients";
	private final String FETCH_SQL_BY_ID = "select * from clients where id = ?";
	private final String UPDATE_SQL = "UPDATE clients SET nom=?, prenom=?, date_naissance=?, e_mail=? WHERE id=?;";
	private final String DELETE_SQL = "DELETE FROM `clients` WHERE id=?;";

	public ClientDaoImpl() {
		String url = "jdbc:mysql://localhost/application?useSSL=false";
		String user = "root";
		String password = "";
		
		try {
			connection = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Client> findAll() {
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(FETCH_SQL);
			ResultSet result = statement.executeQuery();

			List<Client> lesClients = new ArrayList<Client>();
			
			while (result.next()) {
				  int id = result.getInt("id");
				  String nom = result.getString("nom");
				  String prenom = result.getString("prenom");
				  Date lm = result.getDate("date_naissance");
				  String dateNaissance = new SimpleDateFormat("yyyy-mm-dd").format(lm);
				  String email = result.getString("e_mail");

				  Client client = new Client(id, nom, prenom, dateNaissance, email);

				  lesClients.add(client);
				}
			
			return lesClients;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public Client create(Client user) {
		// TODO Auto-generated method stub
		return null;
	}

	public Client findClientById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateClient(Client client) {
		// TODO Auto-generated method stub
		
	}

	public void deleteClient(int id) {
		// TODO Auto-generated method stub
		
	}

}

