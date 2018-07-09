package com.carto.apptemoinSimple.config;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import com.carto.apptemoinSimple.model.Client;
import com.carto.apptemoinSimple.service.ClientController;

public class Application {
	
	public static void main(String[] args) {	
		ClientController clientController = new ClientController();
		List<Client> lesClients = clientController.getAll();
		
		for(Client c:lesClients) {
			System.out.println(c.toString());
		}
		
	}

}