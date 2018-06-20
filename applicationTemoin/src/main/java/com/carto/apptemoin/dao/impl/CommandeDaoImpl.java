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

import com.carto.apptemoin.dao.CommandeDao;
import com.carto.apptemoin.dao.ProduitDao;
import com.carto.apptemoin.model.Commande;

@Repository
public class CommandeDaoImpl implements CommandeDao{

	private final String INSERT_SQL = "INSERT INTO COMMANDES(client_id,produit_id,qte,date_commande) values(?,?,?,?);";
	private final String FETCH_SQL = "select client_id, produit_id, numero, qte, date_commande from commandes;";
	private final String FETCH_SQL_BY_ID = "select * from commandes where client_id = ? AND produit_id=? AND numero=?;";
	private final String UPDATE_SQL = "UPDATE commandes SET qte=?, date_commande=? WHERE client_id = ? AND produit_id=? AND numero=?;";
	private final String DELETE_SQL = "DELETE FROM `commandes` WHERE client_id = ? AND produit_id=? AND numero=?;";
	KeyHolder keyHolder = new GeneratedKeyHolder();
	
	@Autowired
	private ProduitDao produitDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public Commande create(Commande commande) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, commande.getClientId());
				ps.setString(2, commande.getProduitId());
				ps.setInt(3, commande.getQte());
				ps.setString(4, commande.getDateCommande());
				produitDao.updateProduit(commande.getQte(), commande.getProduitId());
				
				return ps;
			}
		}, keyHolder);
		int numero = keyHolder.getKey().intValue();
		commande.setNumero(numero);
		
		return commande;
	}

	@Override
	public List<Commande> findAll() {
		return jdbcTemplate.query(FETCH_SQL, new CommandeMapper());
	}

	@Override
	public Commande findCommandeById(int client_id,String produit_id,int numero) {
		return jdbcTemplate.queryForObject(FETCH_SQL_BY_ID, new Object[] { client_id,produit_id,numero }, new CommandeMapper());
	}

	@Override
	public void updateCommande(Commande commande) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(UPDATE_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, commande.getQte());
				ps.setString(2, commande.getDateCommande());
				ps.setInt(3, commande.getClientId());
				ps.setString(4,  commande.getProduitId());
				ps.setInt(5,  commande.getNumero());
				return ps;
			}
		});
	}

	@Override
	public void deleteCommande(int client_id,String produit_id,int numero) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(DELETE_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, client_id);
				ps.setString(2, produit_id);
				ps.setInt(3, numero);
				return ps;
			}
		});		
	}

}

class CommandeMapper implements RowMapper<Commande> {

	@Override
	public Commande mapRow(ResultSet rs, int rowNum) throws SQLException {
		Commande commande = new Commande();
		commande.setClientId(rs.getInt("client_id"));
		commande.setProduitId(rs.getString("produit_id"));
		commande.setNumero(rs.getInt("numero"));
		commande.setQte(rs.getInt("qte"));
		commande.setDateCommande(rs.getString("date_commande"));
		return commande;
	}

}