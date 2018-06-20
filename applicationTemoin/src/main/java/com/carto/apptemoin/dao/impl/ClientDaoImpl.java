package com.carto.apptemoin.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.carto.apptemoin.dao.ClientDao;
import com.carto.apptemoin.model.Client;

/**
 * @author only2dhir
 *
 */
@Repository
public class ClientDaoImpl implements ClientDao {

	private final String INSERT_SQL = "INSERT INTO CLIENTS(nom,prenom,date_naissance,e_mail) values(?,?,?,?)";
	private final String FETCH_SQL = "select id, nom, prenom, date_naissance, e_mail from clients";
	private final String FETCH_SQL_BY_ID = "select * from clients where id = ?";
	private final String UPDATE_SQL = "UPDATE clients SET nom=?, prenom=?, date_naissance=?, e_mail=? WHERE id=?;";
	private final String DELETE_SQL = "DELETE FROM `clients` WHERE id=?;";
	KeyHolder keyHolder = new GeneratedKeyHolder();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public Client create(final Client client) {
		List<Client> lesClients = findAll();
		boolean creer = true;
		for(Client leClient:lesClients) {
			System.out.println(leClient.getEmail());
			if(leClient.getEmail().equals(client.getEmail())) {
				creer = false;
				try {
					throw new Exception("L'adresse e-mail est déjà dans la base");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		if(client.getDateNaissance() == null || client.getEmail() == null || client.getNom() == null || client.getPrenom() == null){
			creer = false;
			try {
				throw new Exception("Veuillez renseigner tous les champs");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(creer) {
			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, client.getNom());
					ps.setString(2, client.getPrenom());
					ps.setString(3, client.getDateNaissance());
					ps.setString(4, client.getEmail());
					
					return ps;
				}
			}, keyHolder);
			int id = keyHolder.getKey().intValue();
			client.setId(id);
		}
		return client;
	}

	public List<Client> findAll() {
		return jdbcTemplate.query(FETCH_SQL, new ClientMapper());
		
	}

	public Client findClientById(int id) {
		return jdbcTemplate.queryForObject(FETCH_SQL_BY_ID, new Object[] { id }, new ClientMapper());
	}

	public void updateClient(Client client) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(UPDATE_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, client.getNom());
				ps.setString(2, client.getPrenom());
				ps.setString(3, client.getDateNaissance());
				ps.setString(4, client.getEmail());
				ps.setInt(5, client.getId());
				return ps;
			}
		});
	}

	public void deleteClient(int id) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(DELETE_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, id);
				return ps;
			}
		});
	}

}

class ClientMapper implements RowMapper<Client> {

	@Override
	public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
		Client client = new Client();
		client.setId(rs.getInt("id"));
		client.setNom(rs.getString("nom"));
		client.setPrenom(rs.getString("prenom"));
		client.setDateNaissance(rs.getString("date_naissance"));
		client.setEmail(rs.getString("e_mail"));
		return client;
	}

}
