package com.carto.apptemoinSimple.dao;

import java.util.List;

import com.carto.apptemoinSimple.model.Client;

/**
 * @author only2dhir
 *
 */
public interface ClientDao {

	public Client create(final Client user);

	public List<Client> findAll();

	public Client findClientById(int id);
	
	public void updateClient (Client client);
	
	public void deleteClient (int id);

}
