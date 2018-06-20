package com.carto.apptemoin.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carto.apptemoin.config.Application;
import com.carto.apptemoin.dao.CommandeDao;
import com.carto.apptemoin.model.Commande;

/**
 * @author only2dhir
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=Application.class)
public class CommandeDaoTest {
	
	@Autowired
	private CommandeDao commandeDao;

	@Test
	public void createCommande() {
		Commande savedCommande = commandeDao.create(getCommande());
	/*	Commande commandeFromDb = commandeDao.findCommandeById(savedCommande.getId());
		assertEquals(savedCommande.getNom(), commandeFromDb.getNom());
		assertEquals(savedCommande.getEmail(), commandeFromDb.getEmail());
	*/
	}

	@Test
	public void findAllCommandes() {
		List<Commande> commandes = commandeDao.findAll();
		assertNotNull(commandes);
		assertTrue(commandes.size() > 0);
	}

	@Test
	public void findCommandeById() {
		Commande commande = commandeDao.findCommandeById(1,"AS001",1);
		assertNotNull(commande);
	}
	
	private Commande getCommande() {
		Commande commande = new Commande();
		commande.setClientId(1);
		commande.setProduitId("AS001");
		commande.setQte(3);
		commande.setDateCommande("2017-12-19");
		return commande;
	}
	
	@Test
	public void updateCommande() {
		Commande commande = new Commande();
		commande.setClientId(1);
		commande.setProduitId("AJ001");
		commande.setDateCommande("2017-12-19");
		commande.setQte(2);
		
		commandeDao.updateCommande(commande);
	}
	
	@Test
	public void deleteCommande() {
		int clientId = 1;
		String produitId = "AJ001";
		int numero = 1;
		commandeDao.deleteCommande(clientId,produitId,numero);
	}
}
