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
import org.springframework.stereotype.Repository;

import com.carto.apptemoin.dao.ProduitDao;
import com.carto.apptemoin.model.Produit;

/**
 * @author only2dhir
 *
 */
@Repository
public class ProduitDaoImpl implements ProduitDao {

	private final String INSERT_SQL = "INSERT INTO PRODUITS(id,titre,prix,stock) values(?,?,?,?)";
	private final String FETCH_SQL = "select id, titre, prix, stock from produits";
	private final String FETCH_SQL_BY_ID = "select * from produits where id = ?";
	private final String UPDATE_SQL = "UPDATE produits SET titre=?, prix=?, stock=? WHERE id=?;";
	private final String REMOVE_SQL = "UPDATE produits SET stock=? WHERE id=?;";
	private final String DELETE_SQL = "DELETE FROM `produits` WHERE id=?;";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public Produit create(Produit produit) {
		List<Produit> lesProduits = findAll();
		boolean nom = true;
		boolean creer = true;
		if(produit.getId() == null || produit.getTitre() == null || produit.getPrix() < 0 || produit.getStock() < 0) {
			nom = false;
			creer = false;
			try {
				throw new Exception("Tous les champs doivent être remplis");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(nom) {
			for(Produit leProduit :lesProduits) {
				if(produit.getId().equals(leProduit.getId())) {
					creer = false;
					try {
						throw new Exception("La référence existe déjà dans la base");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}
		if(creer){
			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, produit.getId());
					ps.setString(2, produit.getTitre());
					ps.setDouble(3, produit.getPrix());
					ps.setInt(4, produit.getStock());
					return ps;
				}
			});
		}
		return produit;
	}

	@Override
	public List<Produit> findAll() {
		return jdbcTemplate.query(FETCH_SQL, new ProduitMapper());
	}

	@Override
	public Produit findProduitById(String id) {
		return jdbcTemplate.queryForObject(FETCH_SQL_BY_ID, new Object[] { id }, new ProduitMapper());
	}

	@Override
	public void updateProduit(Produit produit) {
		boolean creer = true;
		if(produit.getStock()<0) {
			creer = false;
			try {
				throw new Exception("La qte en stock ne peut pas passer en-dessous de 0");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(creer) {
			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(UPDATE_SQL, Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, produit.getTitre());
					ps.setDouble(2, produit.getPrix());
					ps.setInt(3, produit.getStock());
					ps.setString(4, produit.getId());
					return ps;
				}
			});
		}
	}
	
	@Override
	public void updateProduit(int numero, String id) {
		Produit produit = findProduitById(id);
		if(produit.getStock()<numero) {
			try {
				throw new Exception("Quantité demandée trop importante. Commande impossible");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			jdbcTemplate.update(new PreparedStatementCreator(){
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(REMOVE_SQL, Statement.RETURN_GENERATED_KEYS);
					ps.setInt(1, produit.getStock()-numero);
					ps.setString(2, id);
					
					return ps;
				}
			});
		}
	}

	@Override
	public void deleteProduit(String id) {
		jdbcTemplate.update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(DELETE_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, id);
				return ps;
			}
		});		
	}
	

}

class ProduitMapper implements RowMapper<Produit> {

	@Override
	public Produit mapRow(ResultSet rs, int rowNum) throws SQLException {
		Produit produit = new Produit();
		produit.setId(rs.getString("id"));
		produit.setTitre(rs.getString("titre"));
		produit.setPrix(rs.getDouble("prix"));
		produit.setStock(rs.getInt("stock"));
		return produit;
	}

}
