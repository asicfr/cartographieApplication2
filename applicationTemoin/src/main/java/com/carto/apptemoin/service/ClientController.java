package com.carto.apptemoin.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carto.apptemoin.config.Application;
import com.carto.apptemoin.dao.ClientDao;
import com.carto.apptemoin.model.Client;

@RestController
public class ClientController {

	private final static Logger log = LoggerFactory.getLogger(Application.class.getClass());
	
    @Autowired
    ClientDao clientDAO;

    @RequestMapping(value = "/client", method = RequestMethod.POST)
    public int createClient(@RequestBody Client client) {
    	log.info(">>> post /client call");
    	Client createdClient = clientDAO.create(client);
        return createdClient.getId();
    }
    
    @RequestMapping("/client")
    public List<Client> getAll() {
		System.getProperties().forEach((key, value) -> { System.out.println( "Key: " + key + " = " + value ); });
    	log.info(">>> get /client call");
    	List<Client> lesClients = clientDAO.findAll();
        return lesClients;
    }

    @RequestMapping("/client/{id}")
    public Client getClientById(@PathVariable("id") int id) {
    	log.info(">>> get /client/{id} call");
    	Client client = clientDAO.findClientById(id);
        return client;
    }
    
    @RequestMapping(value = "/client", method = RequestMethod.PUT)
    public void updateClient(@RequestBody Client client) {
    	clientDAO.updateClient(client);
    }
    
    @RequestMapping(value = "/client", method = RequestMethod.DELETE)
    public void deleteClient(@RequestParam("id") int id) {
    	clientDAO.deleteClient(id);
    }
    
}