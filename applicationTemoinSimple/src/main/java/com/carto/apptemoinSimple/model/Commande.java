package com.carto.apptemoinSimple.model;

import java.util.ArrayList;
import java.util.List;

public class Commande {
	private int client_id;
	private String produit_id;
	private int numero;
	private int qte;
	private String date_commande;
	private float prix_total;
	
	public Commande() {
		super();
	}

	public Commande(int client_id, String produit_id, String date_commande) {
		super();
		this.client_id = client_id;
		this.produit_id = produit_id;
		this.date_commande = date_commande;
	}

	public int getClientId() {
		return client_id;
	}

	public void setClientId(int client_id) {
		this.client_id = client_id;
	}

	public String getProduitId() {
		return produit_id;
	}

	public void setProduitId(String produit_id) {
		this.produit_id = produit_id;
	}

	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}

	public int getQte() {
		return qte;
	}

	public void setQte(int qte) {
		this.qte = qte;
	}

	public String getDateCommande() {
		return date_commande;
	}

	public void setDateCommande(String date_commande) {
		this.date_commande = date_commande;
	}

	public float getPrixTotal() {
		return prix_total;
	}

	public void setPrixTotal(float prix_total) {
		this.prix_total = prix_total;
	}
	
}
