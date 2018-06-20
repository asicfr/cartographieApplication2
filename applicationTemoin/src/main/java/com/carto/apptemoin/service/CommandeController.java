package com.carto.apptemoin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.carto.apptemoin.dao.CommandeDao;
import com.carto.apptemoin.model.Commande;

@RestController
public class CommandeController {

    @Autowired
    CommandeDao commandeDAO;

    @RequestMapping(value = "/commande", method = RequestMethod.POST)
    public Commande createCommande(@RequestBody Commande commande) {
    	Commande createdCommande = commandeDAO.create(commande);
        return createdCommande;
    }

    @RequestMapping("/commande")
    public List<Commande> commande() {
    	List<Commande> lesCommandes = commandeDAO.findAll();
        return lesCommandes;
    }

    @RequestMapping("/commande/{clientId}/{produitId}/{numero}")
    public Commande getCommandeById(@PathVariable("clientId") int clientId,@PathVariable("produitId") String produitId,@PathVariable("numero") int numero) {
    	Commande commande = commandeDAO.findCommandeById(clientId,produitId,numero);
        return commande;
    }
    
    @RequestMapping(value = "/commande", method = RequestMethod.PUT)
    public void updateCommande(@RequestBody Commande commande) {
    	commandeDAO.updateCommande(commande);
    }
    
    @RequestMapping(value = "/commande/{clientId}/{produitId}/{numero}", method = RequestMethod.DELETE)
    public void deleteCommande(@PathVariable("clientId") int clientId,@PathVariable("produitId") String produitId,@PathVariable("numero") int numero) {
    	commandeDAO.deleteCommande(clientId,produitId,numero);
    }
}