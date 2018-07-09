package com.carto.apptemoinSimple.service;

import java.util.List;

import com.carto.apptemoinSimple.config.Application;
import com.carto.apptemoinSimple.dao.ClientDao;
import com.carto.apptemoinSimple.dao.DAOFactory;
import com.carto.apptemoinSimple.model.Client;

public class ClientController {

//	private final static Logger log = LoggerFactory.getLogger(Application.class.getClass());
	
    ClientDao clientDAO = DAOFactory.getClientDao();

    
    public List<Client> getAll() {
    	List<Client> lesClients = clientDAO.findAll();
        return lesClients;
    }
    
}