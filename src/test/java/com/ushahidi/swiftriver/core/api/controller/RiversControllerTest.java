/**
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ushahidi.swiftriver.core.api.controller;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

public class RiversControllerTest extends AbstractControllerTest {

	@Test
	@Transactional
	public void createRiver() throws Exception {
		String postBody = "{\"name\":\"Viva Riva\",\"description\":\"Like the movie\",\"public\":true}";

		this.mockMvc
				.perform(
						post("/v1/rivers").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	@Transactional
	public void createRiverWithoutQuota() throws Exception {
		String postBody = "{\"name\":\"Viva Riva\",\"description\":\"Like the movie\",\"public\":true}";

		this.mockMvc.perform(
				post("/v1/rivers").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user2"))).andExpect(
				status().isForbidden());
	}

	@Test
	@Transactional
	public void createRiverWithoutDuplicateName() throws Exception {
		String postBody = "{\"name\":\"Public River 1\",\"description\":\"Like the movie\",\"public\":true}";

		this.mockMvc
				.perform(
						post("/v1/rivers").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	public void getRiverWithNonExistentId() throws Exception {
		this.mockMvc.perform(get("/v1/rivers/9999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	public void getRiverById() throws Exception {
		this.mockMvc
				.perform(get("/v1/rivers/1"))
				.andExpect(status().isOk())
				.andExpect(
						content().contentType("application/json;charset=UTF-8"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.account.id").value(3))
				.andExpect(jsonPath("$.account.account_path").value("user1"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.full").value(false))
				.andExpect(jsonPath("$.name").value("Public River 1"))
				.andExpect(jsonPath("$.follower_count").value(0))
				.andExpect(jsonPath("$.public").value(true))
				.andExpect(jsonPath("$.drop_count").value(6))
				.andExpect(jsonPath("$.drop_quota").value(10000))
				.andExpect(jsonPath("$.extension_count").value(0))
				.andExpect(jsonPath("$.max_drop_id").value(5))
				.andExpect(jsonPath("$.channels").isArray());
	}

	@Test
	public void getDropsFromNonExistentRiver() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		this.mockMvc
				.perform(get("/v1/rivers/9999/drops").principal(authentication))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	public void getDropsFromEmptyRiver() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user2", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		this.mockMvc
				.perform(get("/v1/rivers/2/drops").principal(authentication))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").value(empty()));
	}

	@Test
	public void getDrops() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		this.mockMvc
				.perform(get("/v1/rivers/1/drops").principal(authentication))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].title").exists())
				.andExpect(jsonPath("$[0].content").exists())
				.andExpect(jsonPath("$[0].channel").exists())
				.andExpect(jsonPath("$[0].original_id").exists())
				.andExpect(jsonPath("$[0].original_url").exists())
				// FIXME:.andExpect(jsonPath("$[0].original_place").exists())
				.andExpect(jsonPath("$[0].source").exists())
				.andExpect(jsonPath("$[0].date_published").exists())
				// FIXME:.andExpect(jsonPath("$[0].user_score").exists())
				.andExpect(jsonPath("$[0].comment_count").exists())
				// FIXME: .andExpect(jsonPath("$[0].buckets").isArray())
				.andExpect(jsonPath("$[0].tags").isArray())
				.andExpect(jsonPath("$[0].links").isArray())
				.andExpect(jsonPath("$[0].media").isArray())
				.andExpect(jsonPath("$[0].places").isArray());
	}

	@Test
	public void getDropsWithInvalidChannelIds() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?channel_ids=meow").principal(
								authentication))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").exists())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[0].field").value("channel_ids"))
				.andExpect(jsonPath("$.errors[0].code").value("invalid"));
	}

	@Test
	public void getDropsForSpecificChannelId() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?channel_ids=2").principal(
								authentication)).andExpect(status().isOk())

				.andExpect(jsonPath("$[1].id").value(4));
	}

	@Test
	public void getDropsForSpecificChannelName() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?channels=rss").principal(
								authentication)).andExpect(status().isOk())

				.andExpect(jsonPath("$[1].id").value(1));
	}

	@Test
	public void getDropsWithInvalidState() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?state=meow").principal(
								authentication))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").exists())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[0].field").value("state"))
				.andExpect(jsonPath("$.errors[0].code").value("invalid"));
	}

	@Test
	public void getReadDrops() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?state=read").principal(
								authentication)).andExpect(status().isOk())

				.andExpect(jsonPath("$[1].id").value(2));
	}

	@Test
	public void getUnreadDrops() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		this.mockMvc
				.perform(
						get("/v1/rivers/1/drops?state=unread").principal(
								authentication)).andExpect(status().isOk())

				.andExpect(jsonPath("$[1].id").value(3));
	}

	@Test
	public void getDropsWithinRange() throws Exception {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"user1", "password");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		this.mockMvc
				.perform(
						get(
								"/v1/rivers/1/drops?date_from=01-JAN-12&date_to=01-JAN-13")
								.principal(authentication))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$[0].id").value(4));
	}

	/**
	 * Test for {@link RiversController#deleteRiver(Long)}
	 * 
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void deleteRiver() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1")).andExpect(status().isOk());
	}

	/**
	 * Test for {@link RiversController#deleteRiver(Long)} where the specified
	 * does not exist in which case a 404 should be returned
	 * 
	 * @throws Exception
	 */
	@Test
	public void deleteNonExistentRiver() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/500"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").exists());
	}

	/**
	 * Test for {@link RiversController#getCollaborators(Long)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void getCollaborators() throws Exception {
		this.mockMvc
				.perform(get("/v1/rivers/1/collaborators"))
				.andExpect(status().isOk())
				.andExpect(
						content().contentType("application/json;charset=UTF-8"))
				.andExpect(jsonPath("$").value(hasSize(2)))
				.andExpect(jsonPath("$[1].id").value(2))
				.andExpect(jsonPath("$[1].active").value(true))
				.andExpect(jsonPath("$[1].read_only").value(true))
				.andExpect(jsonPath("$[1].account.id").value(4))
				.andExpect(jsonPath("$[1].account.account_path").value("user2"))
				.andExpect(jsonPath("$[1].account.owner.name").value("User 2"))
				.andExpect(
						jsonPath("$[1].account.owner.avatar")
								.value("https://secure.gravatar.com/avatar/ee8ce7cae1c9d064b9a2c049ce4a1071?s=80&d=mm&r=g"));
	}

	@Test
	public void getUnknownCollaboratorsFromNonExistentRiver() throws Exception {
		this.mockMvc.perform(get("/v1/rivers/9999/collaborators")).andExpect(
				status().isNotFound());
	}

	@Test
	public void addCollaboratorToNonExistentRiver() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 9999}}";

		this.mockMvc.perform(
				post("/v1/rivers/1234/collaborators").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user1"))).andExpect(
				status().isNotFound());
	}

	@Test
	public void addCollaboratorWithoutPermission() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 9999}}";

		this.mockMvc.perform(
				post("/v1/rivers/1/collaborators").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user3"))).andExpect(
				status().isForbidden());
	}

	@Test
	public void addCollaboratorWithMissingAccountField() throws Exception {
		String postBody = "{\"read_only\":true}";

		this.mockMvc
				.perform(
						post("/v1/rivers/1/collaborators").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user3")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[0].field").value("account"))
				.andExpect(jsonPath("$.errors[0].code").value("missing"));
		;
	}

	@Test
	public void addCollaborator() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 5}}";

		this.mockMvc
				.perform(
						post("/v1/rivers/1/collaborators").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(greaterThan(2)));
	}

	@Test
	public void modifyCollaboratorInNonExistentRiver() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 9999}}";

		this.mockMvc.perform(
				put("/v1/rivers/1234/collaborators/1").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user1"))).andExpect(
				status().isNotFound());
	}

	@Test
	public void modifyNonExistentCollaborator() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 9999}}";

		this.mockMvc.perform(
				put("/v1/rivers/1/collaborators/1234").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user1"))).andExpect(
				status().isNotFound());
	}

	@Test
	public void modifyCollaboratorWithoutPermission() throws Exception {

		String postBody = "{\"read_only\":true,\"account\":{\"id\": 4}}";

		this.mockMvc.perform(
				put("/v1/rivers/1/collaborators/2").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user3"))).andExpect(
				status().isForbidden());
	}

	@Test
	public void modifyCollaboratorWithMissingParameters() throws Exception {

		String postBody = "{}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1/collaborators/1").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[0].field").value("read_only"))
				.andExpect(jsonPath("$.errors[0].code").value("missing"))
				.andExpect(jsonPath("$.errors[1].field").value("active"))
				.andExpect(jsonPath("$.errors[1].code").value("missing"));
	}

	@Test
	public void modifyCollaborator() throws Exception {

		String postBody = "{\"read_only\":true}";

		this.mockMvc.perform(
				put("/v1/rivers/1/collaborators/2").content(postBody)
						.contentType(MediaType.APPLICATION_JSON)
						.principal(getAuthentication("user1"))).andExpect(
				status().isOk());
	}

	/**
	 * Test for {@link RiversController#deleteCollaborator(Long, Long)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void deleteCollaborator() throws Exception {
		this.mockMvc.perform(
				delete("/v1/rivers/1/collaborators/2").principal(
						getAuthentication("user1"))).andExpect(status().isOk());
	}

	@Test
	public void deleteCollaboratorInNonExistentRiver() throws Exception {
		this.mockMvc.perform(
				delete("/v1/rivers/1234/collaborators/2").principal(
						getAuthentication("user1"))).andExpect(
				status().isNotFound());
	}

	@Test
	public void deleteNonExistentCollaborator() throws Exception {
		this.mockMvc.perform(
				delete("/v1/rivers/1/collaborators/1234").principal(
						getAuthentication("user1"))).andExpect(
				status().isNotFound());
	}

	@Test
	public void deleteCollaboratorWithoutPermission() throws Exception {
		this.mockMvc.perform(
				delete("/v1/rivers/1/collaborators/1").principal(
						getAuthentication("user3"))).andExpect(
				status().isForbidden());
	}

	/**
	 * Test for {@link RiversController#getFollowers(Long)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void getFollowers() throws Exception {
		this.mockMvc.perform(get("/v1/rivers/1/followers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[*]").isArray());
	}

	/**
	 * Test for {@link RiversController#deleteFollower(Long, Long)}
	 * 
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void deleteFollower() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1/followers/4")).andExpect(
				status().isOk());
	}

	@Test
	@Transactional
	public void createChannel() throws Exception {
		String postBody = "{\"channel\":\"rss\",\"parameters\":\"Like the movie\"}";

		this.mockMvc
				.perform(
						post("/v1/rivers/1/channels").content(postBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.channel").value("rss"));
	}

	@Test
	@Transactional
	public void deleteChannel() throws Exception {
		this.mockMvc.perform(
				delete("/v1/rivers/1/channels/3").contentType(
						MediaType.APPLICATION_JSON).principal(
						getAuthentication("user1"))).andExpect(status().isOk());
	}

	@Test
	@Transactional
	public void modifyChannel() throws Exception {
		String putBody = "{\"channel\":\"rss\",\"active\":true,\"parameters\":\"Rike the movie\"}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1/channels/1").content(putBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.channel").value("rss"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.parameters").value("Rike the movie"));
	}

	@Test
	@Transactional
	public void modifyRiver() throws Exception {
		String putBody = "{\"name\":\"doof twaf\",\"description\":\"asi asi\",\"public\":true}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1").content(putBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("doof twaf"))
				.andExpect(jsonPath("$.description").value("asi asi"))
				.andExpect(jsonPath("$.public").value(true));
	}

	@Test
	@Transactional
	public void modifyRiverPartially() throws Exception {
		String putBody = "{\"public\":false}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1").content(putBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Public River 1"))
				.andExpect(
						jsonPath("$.description").value("Just a public river"))
				.andExpect(jsonPath("$.public").value(false));
	}

	@Test
	public void modifyRiverWithoutPermission() throws Exception {
		String putBody = "{\"public\":false}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1").content(putBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user3")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	public void modifyRiverToExistingRiverName() throws Exception {
		String putBody = "{\"name\":\"Private River 1\"}";

		this.mockMvc
				.perform(
						put("/v1/rivers/1").content(putBody)
								.contentType(MediaType.APPLICATION_JSON)
								.principal(getAuthentication("user1")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").isArray());
	}
	
	@Test
	public void addDropTag() throws Exception {
		String tag = "{\"tag\": \"Samuel L. Jackson\", \"tag_type\":\"person\"}";
		this.mockMvc.perform(post("/v1/rivers/1/drops/1/tags")
				.content(tag)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tag").value("Samuel L. Jackson"));
	}
	
	@Test
	public void deleteDropTag() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1/drops/1/tags/2")
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk());
	}

	@Test
	public void addDropLink() throws Exception {
		String link = "{\"url\": \"http://www.mashada.com\"}";
		this.mockMvc.perform(post("/v1/rivers/1/drops/1/links")
				.content(link)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value("http://www.mashada.com"));
	}

	@Test
	public void deleteDropLink() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1/drops/1/links/2")
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk());
	}
	
	@Test
	public void addDropPlace() throws Exception {
		String place = "{\"name\": \"Mombasa\", \"latitude\":-4.05466, \"longitude\": 39.6636 }";

		this.mockMvc.perform(post("/v1/rivers/1/drops/1/places")
				.content(place)
				.contentType(MediaType.APPLICATION_JSON)
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Mombasa"))
			.andExpect(jsonPath("$.latitude").value(-4.05466));
	}
	
	@Test
	public void deleteDropPlace() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1/drops/1/places/2")
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk());
	}

	@Test
	public void addDropComment() throws Exception {
		String comment = "{\"comment_text\": \"River 1 Drop 1 Comment\"}";

		this.mockMvc.perform(post("/v1/rivers/1/drops/1/comments")
				.principal(getAuthentication("user1"))
				.content(comment)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.comment_text").value("River 1 Drop 1 Comment"));
	}

	@Test
	public void deleteDropComment() throws Exception {
		this.mockMvc.perform(delete("/v1/rivers/1/drops/1/comments/3")
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk());
	}
	
	@Test
	public void getDropComments() throws Exception {
		this.mockMvc.perform(get("/v1/rivers/1/drops/1/comments")
				.principal(getAuthentication("user1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.[*]").isArray());
	}

}
