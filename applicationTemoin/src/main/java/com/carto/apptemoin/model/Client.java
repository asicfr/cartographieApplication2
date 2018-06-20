package com.carto.apptemoin.model;

import java.util.List;

import com.carto.apptemoin.dao.ClientDao;

public class Client {

	private int id;
	private String nom;
    private String prenom;
    private String date_naissance;
    private String e_mail;

    public Client() {
		super();
	}

	public Client(int id, String nom, String prenom, String date_naissance, String e_mail) {
		super();
		setId(id);
		setNom(nom);
		setPrenom(prenom);
		setDateNaissance(date_naissance);
		setEmail(e_mail);
	}

	public Client(String nom, String prenom, String date_naissance, String e_mail) {
		super();
		setNom(nom);
		setPrenom(prenom);
		setDateNaissance(date_naissance);
		setEmail(e_mail);
	}

	public void setId(int id) {
		this.id = id;
	}
    
    public int getId() {
		return id;
	}

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

	public String getDateNaissance() {
		return date_naissance;
	}

	public void setDateNaissance(String date_naissance) {
		this.date_naissance = date_naissance;
	}

    public String getEmail() {
        return e_mail;
    }

    public void setEmail(String e_mail) {
        this.e_mail = e_mail;
    }

}
