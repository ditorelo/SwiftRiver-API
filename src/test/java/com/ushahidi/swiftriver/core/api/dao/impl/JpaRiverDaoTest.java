/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/agpl.html>
 * 
 * Copyright (C) Ushahidi Inc. All Rights Reserved.
 */
package com.ushahidi.swiftriver.core.api.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.ushahidi.swiftriver.core.api.dao.AccountDao;
import com.ushahidi.swiftriver.core.api.dao.RiverDao;
import com.ushahidi.swiftriver.core.api.dao.UserDao;
import com.ushahidi.swiftriver.core.model.Account;
import com.ushahidi.swiftriver.core.model.River;
import com.ushahidi.swiftriver.core.model.RiverCollaborator;
import com.ushahidi.swiftriver.test.AbstractTransactionalTest;

/**
 * Integration tests for the rivers repository class
 * @author ekala
 *
 */
public class JpaRiverDaoTest extends AbstractTransactionalTest {

	@Autowired
	private RiverDao riverDao;
	
	@Autowired
	private AccountDao accountDao;

	@Autowired
	private UserDao userDao;
	
	@Override
	@Before
	public void beforeTest() {

	}
	
	/**
	 * Verifies that a river is successfully created
	 */
	@Test
	@Transactional
	public void testCreateRiver() {
		River river = new River();
		Account account = accountDao.findByUsername("admin1");
		
		river.setName("Test river");
		river.setUrl("test-river");
		river.setAccount(account);
		river.setRiverPublic(false);
		
		riverDao.save(river);
		
		assertNotNull(river.getId());
	}
	
	/**
	 * Verifies that a collaborator is successfully added to river
	 */
	@Test
	@Transactional
	public void testAddCollaborator() {
		long riverId = 1;

		River river = riverDao.findById(riverId);
		int collaboratorCount = river.getCollaborators().size();

		Account account = accountDao.findByUsername("admin4");		
		
		RiverCollaborator collaborator = new RiverCollaborator();
		collaborator.setAccount(account);
		collaborator.setActive(false);
		collaborator.setReadOnly(true);

		riverDao.addCollaborator(river, collaborator);
		assertEquals(collaboratorCount+1, river.getCollaborators().size());
	}
	
}
