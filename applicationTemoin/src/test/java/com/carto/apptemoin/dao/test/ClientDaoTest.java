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
import com.carto.apptemoin.dao.ClientDao;
import com.carto.apptemoin.model.Client;

/**
 * @author only2dhir
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=Application.class)
public class ClientDaoTest {

	@Autowired
	private ClientDao clientDao;

	@Test
	public void createClient() {
		Client savedClient = clientDao.create(getClient());
	/*	Client clientFromDb = clientDao.findClientById(savedClient.getId());
		assertEquals(savedClient.getNom(), clientFromDb.getNom());
		assertEquals(savedClient.getEmail(), clientFromDb.getEmail());
	*/
	}

	@Test
	public void findAllClients() {
		List<Client> clients = clientDao.findAll();
		assertNotNull(clients);
		assertTrue(clients.size() > 0);
	}

	@Test
	public void findClientById() {
		Client client = clientDao.findClientById(1);
		assertNotNull(client);
	}
	
	private Client getClient() {
		Client client = new Client();
		client.setNom("Doe");
		client.setPrenom("John");
		client.setDateNaissance("1990-03-14");
		client.setEmail("johnloo@gmail.com");
		return client;
	}
	
	@Test
	public void updateClient() {
		Client client = new Client();
		client.setId(1);
		client.setNom("Durand");
		client.setPrenom("Jamy");
		client.setDateNaissance("1986-03-30");
		client.setEmail("jamydurand@gmail.com");
		
		clientDao.updateClient(client);
	}
	
	@Test
	public void deleteClient() {
		int id = 10;
		clientDao.deleteClient(id);
	}

}