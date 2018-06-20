package com.carto.apptemoin.dao;

import java.util.List;

import com.carto.apptemoin.model.Commande;

public interface CommandeDao {

	public Commande create(final Commande commande);

	public List<Commande> findAll();

	public Commande findCommandeById(int client_id, String produit_id, int numero);
	
	public void updateCommande (Commande commande);
	
	public void deleteCommande (int client_id,String produit_id,int numero);

}
