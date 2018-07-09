package com.carto.apptemoinSimple.dao;

import com.carto.apptemoinSimple.dao.ClientDao;

public class DAOFactory {
	
	public static ClientDao getClientDao()  {
		ClientDao clientDao=null;
		try {
			clientDao =(ClientDao ) Class.forName("com.carto.apptemoinSimple.dao.impl.ClientDaoImpl").newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clientDao; 
	}

}
