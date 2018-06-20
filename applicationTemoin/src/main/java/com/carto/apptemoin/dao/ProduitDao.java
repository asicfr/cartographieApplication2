package com.carto.apptemoin.dao;

import java.util.List;

import com.carto.apptemoin.model.Produit;

/**
 * @author only2dhir
 *
 */
public interface ProduitDao {

	public Produit create(final Produit produit);

	public List<Produit> findAll();

	public Produit findProduitById(String id);
	
	public void updateProduit (Produit produit);
	
	public void updateProduit (int numero, String id);
	
	public void deleteProduit (String id);

}
