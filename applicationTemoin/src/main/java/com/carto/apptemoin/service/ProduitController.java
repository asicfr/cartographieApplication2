package com.carto.apptemoin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.carto.apptemoin.dao.ProduitDao;
import com.carto.apptemoin.model.Produit;

@RestController
public class ProduitController {

    @Autowired
    ProduitDao produitDAO;

    @RequestMapping(value = "/produit", method = RequestMethod.POST)
    public String createProduit(@RequestBody Produit produit) {
    	Produit createdProduit = produitDAO.create(produit);
        return createdProduit.getId();
    }

    @RequestMapping("/produit")
    public List<Produit> produit() {
    	List<Produit> lesProduits = produitDAO.findAll();
        return lesProduits;
    }

    @RequestMapping("/produit/{id}")
    public Produit getProduitById(@PathVariable("id") String id) {
    	Produit produit = produitDAO.findProduitById(id);
        return produit;
    }
    
    @RequestMapping(value = "/produit", method = RequestMethod.PUT)
    public void updateProduit(@RequestBody Produit produit) {
    	produitDAO.updateProduit(produit);
    }
    
    @RequestMapping(value = "/produit/{id}", method = RequestMethod.DELETE)
    public void deleteProduit(@PathVariable("id") String id) {
    	produitDAO.deleteProduit(id);
    }
}