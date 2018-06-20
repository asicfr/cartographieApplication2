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
import com.carto.apptemoin.dao.ProduitDao;
import com.carto.apptemoin.model.Produit;

/**
 * @author only2dhir
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=Application.class)
public class ProduitDaoTest {
	
	@Autowired
	private ProduitDao produitDao;

	@Test
	public void createProduit() {
		Produit savedProduit = produitDao.create(getProduit());
	/*	Produit produitFromDb = produitDao.findProduitById(savedProduit.getId());
		assertEquals(savedProduit.getNom(), produitFromDb.getNom());
		assertEquals(savedProduit.getEmail(), produitFromDb.getEmail());
	*/
	}

	@Test
	public void findAllProduits() {
		List<Produit> produits = produitDao.findAll();
		assertNotNull(produits);
		assertTrue(produits.size() > 0);
	}

	@Test
	public void findProduitById() {
		Produit produit = produitDao.findProduitById("CS001");
		assertNotNull(produit);
	}
	
	private Produit getProduit() {
		Produit produit = new Produit();
		produit.setId("AS001");
		produit.setTitre("Aspirateur");
		produit.setPrix(50.19);
		produit.setStock(20);
		return produit;
	}
	
	@Test
	public void updateProduit() {
		Produit produit = new Produit();
		produit.setId("AS001");
		produit.setTitre("Aspirateur");
		produit.setPrix(49.99);
		produit.setStock(18);
		
		produitDao.updateProduit(produit);
	}
	
	@Test
	public void deleteProduit() {
		String id = "AS001";
		produitDao.deleteProduit(id);
	}
}
