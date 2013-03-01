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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ushahidi.swiftriver.core.api.dao.DropDao;
import com.ushahidi.swiftriver.core.model.Account;
import com.ushahidi.swiftriver.core.model.Bucket;
import com.ushahidi.swiftriver.core.model.Drop;
import com.ushahidi.swiftriver.core.model.DropComment;
import com.ushahidi.swiftriver.core.model.DropSource;
import com.ushahidi.swiftriver.core.model.Link;
import com.ushahidi.swiftriver.core.model.Media;
import com.ushahidi.swiftriver.core.model.MediaThumbnail;
import com.ushahidi.swiftriver.core.model.Place;
import com.ushahidi.swiftriver.core.model.Tag;

/**
 * @author ekala
 *
 */
@Repository
public class JpaDropDao extends AbstractJpaDao<Drop> implements DropDao {

	final Logger logger = LoggerFactory.getLogger(JpaDropDao.class);
	
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}
	
	
	/**
	 * @see DropDao#createDrops(Collection)
	 */
	public void createDrops(Collection<Drop> drops) {
		// TODO Auto-generated method stub
	}

	/**
	 * @see DropDao#addLinks(long, Collection)
	 */
	public void addLinks(long dropId, Collection<Link> links) {
		findById(dropId).getLinks().addAll(links);
	}

	/**
	 * @see DropDao#addPlaces(long, Collection)
	 */
	public void addPlaces(long dropId, Collection<Place> places) {
		findById(dropId).getPlaces().addAll(places);
	}

	/**
	 * @see DropDao#addMultipleMedia(long, Collection)
	 */
	public void addMultipleMedia(long dropId, Collection<Media> media) {
		findById(dropId).getMedia().addAll(media);
	}

	/**
	 * @see DropDao#addTags(long, Collection)
	 */
	public void addTags(long dropId, Collection<Tag> tags) {
		findById(dropId).getTags().addAll(tags);
	}

	/**
	 * @see DropDao#findDropsByHash(ArrayList)
	 */
	@SuppressWarnings("unchecked")
	public List<Drop> findDropsByHash(ArrayList<String> dropHashes) {
		String sql = "FROM Drop d WHERE d.dropletHash in (?1)";

		Query query = em.createQuery(sql);
		query.setParameter(1, dropHashes);

		return (List<Drop>) query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ushahidi.swiftriver.core.api.dao.DropDao#populateMetadata(java.util
	 * .List, com.ushahidi.swiftriver.core.model.Account)
	 */
	public void populateMetadata(List<Drop> drops, DropSource dropSource, Account queryingAccount) {
		if (drops.size() == 0) {
			return;
		}

		populateTags(drops, dropSource);
		populateLinks(drops, dropSource);
		populateMedia(drops, dropSource);
		populatePlaces(drops, dropSource);
		populateBuckets(drops, queryingAccount);
	}

	/**
	 * Populate tag metadata into the given drops.
	 * 
	 * @param drops
	 * @param dropSource
	 */
	public void populateTags(List<Drop> drops, DropSource dropSource) {

		List<Long> dropIds = new ArrayList<Long>();
		for (Drop drop : drops) {
			dropIds.add(drop.getId());
		}

		String sql = null;
		switch (dropSource) {
			case RIVER:
				sql = getRiverTagsQuery();
				break;
				
			case BUCKET:
				sql = getBucketTagsQuery();
				break;
		}

		Query query = em.createNativeQuery(sql);
		query.setParameter("drop_ids", dropIds);

		// Group the tags by drop id
		Map<Long, List<Tag>> tags = new HashMap<Long, List<Tag>>();
		for (Object oRow : query.getResultList()) {
			Object[] r = (Object[]) oRow;

			Long dropId = ((BigInteger) r[0]).longValue();
			Tag tag = new Tag();
			tag.setId(((BigInteger) r[1]).longValue());
			tag.setTag((String) r[2]);
			tag.setType((String) r[4]);

			List<Tag> t = tags.get(dropId);
			if (t == null) {
				t = new ArrayList<Tag>();
				tags.put(dropId, t);
			}

			t.add(tag);
		}

		for (Drop drop : drops) {
			List<Tag> t = tags.get(drop.getId());

			if (t != null) {
				drop.setTags(t);
			} else {
				drop.setTags(new ArrayList<Tag>());
			}
		}

	}
	/**
	 * Builds and returns the query for fetching tags for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.Bucket</code>
	 * 
	 * @return
	 */
	private String getBucketTagsQuery() {
		String sql = "SELECT `buckets_droplets`.`id` AS `droplet_id`, `tag_id` AS `id`, `tag`, `tag_canonical`, `tag_type` ";
		sql += "FROM `droplets_tags`  ";
		sql += "INNER JOIN `tags` ON (`tags`.`id` = `tag_id`)  ";
		sql += "INNER JOIN buckets_droplets ON (`buckets_droplets`.`droplet_id` = `droplets_tags`.`droplet_id`)";
		sql += "WHERE `buckets_droplets`.`id` IN :drop_ids  ";
		sql += "AND `tags`.`id` NOT IN ( ";
		sql += "	SELECT `tag_id` FROM `bucket_droplet_tags`  ";
		sql += "	WHERE `buckets_droplets_id` IN :drop_ids  ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL  ";
		sql += "SELECT `buckets_droplets_id` AS `droplet_id`, `tag_id` AS `id`, `tag`, `tag_canonical`, `tag_type`  ";
		sql += "FROM `bucket_droplet_tags` ";
		sql += "INNER JOIN `tags` ON (`tags`.`id` = `tag_id`)  ";
		sql += "WHERE `buckets_droplets_id` IN :drop_ids  ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Builds and returns the query for fetching tags for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.River</code>
	 * 
	 * @return
	 */
	private String getRiverTagsQuery() {
		String sql = "SELECT `rivers_droplets`.`id` AS `droplet_id`, `tag_id` AS `id`, `tag`, `tag_canonical`, `tag_type` ";
		sql += "FROM `droplets_tags`  ";
		sql += "INNER JOIN `tags` ON (`tags`.`id` = `tag_id`)  ";
		sql += "INNER JOIN rivers_droplets ON (`rivers_droplets`.`droplet_id` = `droplets_tags`.`droplet_id`)";
		sql += "WHERE `rivers_droplets`.`id` IN :drop_ids  ";
		sql += "AND `tags`.`id` NOT IN ( ";
		sql += "	SELECT `tag_id` FROM `river_droplet_tags`  ";
		sql += "	WHERE `rivers_droplets_id` IN :drop_ids  ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL  ";
		sql += "SELECT `rivers_droplets_id` AS `droplet_id`, `tag_id` AS `id`, `tag`, `tag_canonical`, `tag_type`  ";
		sql += "FROM `river_droplet_tags` ";
		sql += "INNER JOIN `tags` ON (`tags`.`id` = `tag_id`)  ";
		sql += "WHERE `rivers_droplets_id` IN :drop_ids  ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Populate link metadata into the given drops array.
	 * 
	 * @param drops
	 * @param dropSource
	 */
	public void populateLinks(List<Drop> drops, DropSource dropSource) {

		List<Long> dropIds = new ArrayList<Long>();
		for (Drop drop : drops) {
			dropIds.add(drop.getId());
		}

		String sql = null;
		switch (dropSource) {
			case RIVER:
				sql = getRiverLinksQuery();
				break;
				
			case BUCKET:
				sql = getBucketLinksQuery();
				break;
		}

		Query query = em.createNativeQuery(sql);
		query.setParameter("drop_ids", dropIds);

		// Group the links by drop id
		Map<Long, List<Link>> links = new HashMap<Long, List<Link>>();
		for (Object oRow : query.getResultList()) {
			Object[] r = (Object[]) oRow;

			Long dropId = ((BigInteger) r[0]).longValue();
			Link link = new Link();
			link.setId(((BigInteger) r[1]).longValue());
			link.setUrl((String) r[2]);

			List<Link> l = links.get(dropId);
			if (l == null) {
				l = new ArrayList<Link>();
				links.put(dropId, l);
			}

			l.add(link);
		}

		for (Drop drop : drops) {
			List<Link> l = links.get(drop.getId());

			if (l != null) {
				drop.setLinks(l);
			} else {
				drop.setLinks(new ArrayList<Link>());
			}
		}
	}

	/**
	 * Builds and returns the query for fetching links for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.Bucket</code>
	 * 
	 * @return
	 */
	private String getBucketLinksQuery() {
		String sql = "SELECT `buckets_droplets`.`id` AS `droplet_id`, `link_id` AS `id`, `url` ";
		sql += "FROM `droplets_links`  ";
		sql += "INNER JOIN `links` ON (`links`.`id` = `link_id`)  ";
		sql += "INNER JOIN buckets_droplets ON (`buckets_droplets`.`droplet_id` = `droplets_links`.`droplet_id`)";
		sql += "WHERE `buckets_droplets`.`id` IN :drop_ids  ";
		sql += "AND `links`.`id` NOT IN ( ";
		sql += "	SELECT `link_id` FROM `bucket_droplet_links`  ";
		sql += "	WHERE `buckets_droplets_id` IN :drop_ids  ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL  ";
		sql += "SELECT `buckets_droplets_id` AS `droplet_id`, `link_id` AS `id`, `url`  ";
		sql += "FROM `bucket_droplet_links`  ";
		sql += "INNER JOIN `links` ON (`links`.`id` = `link_id`)  ";
		sql += "WHERE `buckets_droplets_id` IN :drop_ids  ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Builds and returns the query for fetching links for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.River</code>
	 * 
	 * @return
	 */
	private String getRiverLinksQuery() {
		String sql = "SELECT `rivers_droplets`.`id` AS `droplet_id`, `link_id` AS `id`, `url` ";
		sql += "FROM `droplets_links`  ";
		sql += "INNER JOIN `links` ON (`links`.`id` = `link_id`)  ";
		sql += "INNER JOIN rivers_droplets ON (`rivers_droplets`.`droplet_id` = `droplets_links`.`droplet_id`)";
		sql += "WHERE `rivers_droplets`.`id` IN :drop_ids  ";
		sql += "AND `links`.`id` NOT IN ( ";
		sql += "	SELECT `link_id` FROM `river_droplet_links`  ";
		sql += "	WHERE `rivers_droplets_id` IN :drop_ids  ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL  ";
		sql += "SELECT `rivers_droplets_id` AS `droplet_id`, `link_id` AS `id`, `url`  ";
		sql += "FROM `river_droplet_links`  ";
		sql += "INNER JOIN `links` ON (`links`.`id` = `link_id`)  ";
		sql += "WHERE `rivers_droplets_id` IN :drop_ids  ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Populate media metadata into the given drops array.
	 * 
	 * @param drops
	 * @param dropSource
	 */
	public void populateMedia(List<Drop> drops, DropSource dropSource) {

		Map<Long, Integer> dropIndex = new HashMap<Long, Integer>();
		int i = 0;
		for (Drop drop : drops) {
			dropIndex.put(drop.getId(), i);
			i++;
		}
		
		// Generate a map for drop images		
		String sql = null;		
		switch (dropSource) {
			case BUCKET:
				sql = "SELECT `buckets_droplets`.`id`, `droplet_image` FROM `droplets` ";
				sql += "INNER JOIN `buckets_droplets` ON (`buckets_droplets`.`droplet_id` = `droplets`.`id`) ";
				sql += "WHERE `buckets_droplets`.`id` IN :drop_ids ";
				break;
				
			case RIVER:
				sql = "SELECT `rivers_droplets`.`id`, `droplet_image` FROM `droplets` ";
				sql += "INNER JOIN `rivers_droplets` ON (`rivers_droplets`.`droplet_id` = `droplets`.`id`) ";
				sql += "WHERE `rivers_droplets`.`id` IN :drop_ids ";
				break;
		}
		
		sql += "AND `droplets`.`droplet_image` > 0";

		Query query = em.createNativeQuery(sql);
		query.setParameter("drop_ids", dropIndex.keySet());

		Map<Long, Long> dropImagesMap = new HashMap<Long, Long>();
		for (Object oRow2 : query.getResultList()) {
			Object[] r2 = (Object[]) oRow2;
			dropImagesMap.put(((BigInteger) r2[0]).longValue(),
					((BigInteger) r2[1]).longValue());
		}

		// Get the query to fetch the drop media
		switch (dropSource) {
			case RIVER:
				sql = getRiverMediaQuery();
				break;
				
			case BUCKET:
				sql = getBucketMediaQuery();
				break;
		}		

		query = em.createNativeQuery(sql);
		query.setParameter("drop_ids", dropIndex.keySet());

		// Group the media by drop id
		for (Object oRow : query.getResultList()) {
			Object[] r = (Object[]) oRow;

			Long dropId = ((BigInteger) r[0]).longValue();
			Drop drop = drops.get(dropIndex.get(dropId));
			if (drop.getMedia() == null) {
				drop.setMedia(new ArrayList<Media>());
			}
		
			Long mediaId = ((BigInteger) r[1]).longValue();
			Media m = null;
			for (Media x : drop.getMedia()) {
				if (x.getId() == mediaId) {
					m = x;
				}
			}
			
			if (m == null) {
				m = new Media();
				m.setId(mediaId);
				m.setUrl((String) r[2]);
				m.setType((String) r[3]);
			} 
			
			// Add thumbnails
			if (r[4] != null) {
				MediaThumbnail mt = new MediaThumbnail();
				mt.setMedia(m);
				mt.setSize((Integer)r[4]);
				mt.setUrl((String)r[5]);
				
				List<MediaThumbnail> thumbnails = m.getThumbnails();
				if (thumbnails == null) {
					thumbnails = new ArrayList<MediaThumbnail>();
					m.setThumbnails(thumbnails);
				}
				thumbnails.add(mt);
			}
			
			if (!drop.getMedia().contains(m)) {
				drop.getMedia().add(m);
				
				// Set the droplet image if any
				Long dropImageId = dropImagesMap.get(drop.getId());
				if (dropImageId != null && dropImageId == m.getId()) {
					drop.setImage(m);
				}
			}			
		}
	}

	/**
	 * Builds and returns the query for fetching media for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.Bucket</code>
	 * 
	 * @return
	 */
	private String getBucketMediaQuery() {
		String sql = "SELECT `buckets_droplets`.`id` AS `droplet_id`, `media`.`id` AS `id`, `media`.`url` AS `url`, `type`, `media_thumbnails`.`size` AS `thumbnail_size`, ";
		sql += "`media_thumbnails`.`url` AS `thumbnail_url` ";
		sql += "FROM `droplets_media` ";
		sql += "INNER JOIN `media` ON (`media`.`id` = `media_id`) ";
		sql += "INNER JOIN `buckets_droplets` ON (`buckets_droplets`.`droplet_id` = `droplets_media`.`droplet_id`) ";
		sql += "LEFT JOIN `media_thumbnails` ON (`media_thumbnails`.`media_id` = `media`.`id`) ";
		sql += "WHERE `buckets_droplets`.`id` IN :drop_ids ";
		sql += "AND `media`.`id` NOT IN ( ";
		sql += "	SELECT `media_id` ";
		sql += "	FROM `bucket_droplet_media` ";
		sql += "	WHERE `buckets_droplets_id` IN :drop_ids ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL ";
		sql += "SELECT `buckets_droplets_id` AS `droplet_id`, `media`.`id` AS `id`, `media`.`url` AS `url`, `type`, `media_thumbnails`.`size` AS `thumbnail_size`, `media_thumbnails`.`url` AS `thumbnail_url` ";
		sql += "FROM `bucket_droplet_media` ";
		sql += "INNER JOIN `media` ON (`media`.`id` = `media_id`) ";
		sql += "LEFT JOIN `media_thumbnails` ON (`media_thumbnails`.`media_id` = `media`.`id`) ";
		sql += "WHERE `buckets_droplets_id` IN :drop_ids ";
		sql += "AND `deleted` = 0; ";
		
		return sql;
	}

	/**
	 * Builds and returns the query for fetching media for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.River</code>
	 * 
	 * @return
	 */
	private String getRiverMediaQuery() {
		String sql = "SELECT `rivers_droplets`.`id` AS `droplet_id`, `media`.`id` AS `id`, `media`.`url` AS `url`, `type`, `media_thumbnails`.`size` AS `thumbnail_size`, ";
		sql += "`media_thumbnails`.`url` AS `thumbnail_url` ";
		sql += "FROM `droplets_media` ";
		sql += "INNER JOIN `media` ON (`media`.`id` = `media_id`) ";
		sql += "INNER JOIN `rivers_droplets` ON (`rivers_droplets`.`droplet_id` = `droplets_media`.`droplet_id`) ";
		sql += "LEFT JOIN `media_thumbnails` ON (`media_thumbnails`.`media_id` = `media`.`id`) ";
		sql += "WHERE `rivers_droplets`.`id` IN :drop_ids ";
		sql += "AND `media`.`id` NOT IN ( ";
		sql += "	SELECT `media_id` ";
		sql += "	FROM `river_droplet_media` ";
		sql += "	WHERE `rivers_droplets_id` IN :drop_ids ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL ";
		sql += "SELECT `rivers_droplets_id` AS `droplet_id`, `media`.`id` AS `id`, `media`.`url` AS `url`, `type`, `media_thumbnails`.`size` AS `thumbnail_size`, `media_thumbnails`.`url` AS `thumbnail_url` ";
		sql += "FROM `river_droplet_media` ";
		sql += "INNER JOIN `media` ON (`media`.`id` = `media_id`) ";
		sql += "LEFT JOIN `media_thumbnails` ON (`media_thumbnails`.`media_id` = `media`.`id`) ";
		sql += "WHERE `rivers_droplets_id` IN :drop_ids ";
		sql += "AND `deleted` = 0; ";
		
		return sql;
	}

	/**
	 * Populate geo metadata into the given drops array.
	 * 
	 * @param drops
	 * @param dropSource
	 */
	public void populatePlaces(List<Drop> drops, DropSource dropSource) {
		
		Map<Long, Integer> dropIndex = new HashMap<Long, Integer>();
		int i = 0;
		for (Drop drop : drops) {
			dropIndex.put(drop.getId(), i);
			i++;
		}

		String sql = null;
		// Get the query to fetch the drop media
		switch (dropSource) {
			case RIVER:
				sql = getRiverPlacesQuery();
				break;
				
			case BUCKET:
				sql = getBucketPlacesQuery();
				break;
		}		

		Query query = em.createNativeQuery(sql);
		query.setParameter("drop_ids", dropIndex.keySet());
		
		// Group the media by drop id
		Map<Long, Place> places = new HashMap<Long, Place>();
		for (Object oRow : query.getResultList()) {
			Object[] r = (Object[]) oRow;

			Long dropId = ((BigInteger) r[0]).longValue();
			Drop drop = drops.get(dropIndex.get(dropId));
			if (drop.getPlaces() == null) {
				drop.setPlaces(new ArrayList<Place>());
			}

			Long placeId = ((BigInteger) r[1]).longValue();
			Place p = places.get(placeId);

			if (p == null) {
				p = new Place();
				p.setId(placeId);
				p.setPlaceName((String) r[2]);
				p.setLatitude((Float)r[5]);
				p.setLongitude((Float)r[6]);

				places.put(placeId, p);
			} 

			// Add place to drop
			if (!drop.getPlaces().contains(p)) {
				drop.getPlaces().add(p);
			}
		}
	}
	
	/**
	 * Builds and returns the query for fetching places for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.Bucket</code>
	 * 
	 * @return
	 */
	private String getBucketPlacesQuery() {
		String sql = "SELECT `buckets_droplets`.`id` AS `droplet_id`, `place_id` AS `id`, `place_name`, `place_name_canonical`, ";
		sql += "`places`.`hash` AS `place_hash`, `latitude`, `longitude` ";
		sql += "FROM `droplets_places` ";
		sql += "INNER JOIN `places` ON (`places`.`id` = `place_id`) ";
		sql += "INNER JOIN `buckets_droplets` ON (`buckets_droplets`.`droplet_id` = `droplets_places`.`droplet_id`) ";
		sql += "WHERE `buckets_droplets`.`id` IN :drop_ids ";
		sql += "AND `places`.`id` NOT IN ( ";
		sql += "	SELECT `place_id` ";
		sql += "	FROM `bucket_droplet_places` ";
		sql += "	WHERE `buckets_droplets_id` IN :drop_ids ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL ";
		sql += "SELECT `buckets_droplets_id` AS `droplet_id`, `place_id` AS `id`, `place_name`, `place_name_canonical`, `places`.`hash` AS `place_hash`, `latitude`, `longitude` ";
		sql += "FROM `bucket_droplet_places` ";
		sql += "INNER JOIN `places` ON (`places`.`id` = `place_id`) ";
		sql += "WHERE `buckets_droplets_id` IN :drop_ids ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Builds and returns the query for fetching places for the drops
	 * in a <code>com.ushahidi.swiftriver.core.model.River</code>
	 * 
	 * @return
	 */
	private String getRiverPlacesQuery() {
		String sql = "SELECT `rivers_droplets`.`id` AS `droplet_id`, `place_id` AS `id`, `place_name`, `place_name_canonical`, ";
		sql += "`places`.`hash` AS `place_hash`, `latitude`, `longitude` ";
		sql += "FROM `droplets_places` ";
		sql += "INNER JOIN `places` ON (`places`.`id` = `place_id`) ";
		sql += "INNER JOIN `rivers_droplets` ON (`rivers_droplets`.`droplet_id` = `droplets_places`.`droplet_id`) ";
		sql += "WHERE `rivers_droplets`.`id` IN :drop_ids ";
		sql += "AND `places`.`id` NOT IN ( ";
		sql += "	SELECT `place_id` ";
		sql += "	FROM `river_droplet_places` ";
		sql += "	WHERE `rivers_droplets_id` IN :drop_ids ";
		sql += "	AND `deleted` = 1) ";
		sql += "UNION ALL ";
		sql += "SELECT `rivers_droplets_id` AS `droplet_id`, `place_id` AS `id`, `place_name`, `place_name_canonical`, `places`.`hash` AS `place_hash`, `latitude`, `longitude` ";
		sql += "FROM `river_droplet_places` ";
		sql += "INNER JOIN `places` ON (`places`.`id` = `place_id`) ";
		sql += "WHERE `rivers_droplets_id` IN :drop_ids ";
		sql += "AND `deleted` = 0 ";
		
		return sql;
	}

	/**
	 * Populates the buckets for each of the {@link Drop} 
	 * in <code>drops</code>
	 * 
	 * @param drops
	 * @param queryingAccount
	 */
	public void populateBuckets(List<Drop> drops, Account queryingAccount) {
		Map<Long, Integer> dropsIndex = new HashMap<Long, Integer>();
		int i = 0;
		for (Drop drop: drops) {
			dropsIndex.put(drop.getId(), i);
			i++;
		}

		// Query to fetch the buckets
		String sql = "SELECT `buckets_droplets`.`id` AS `id`, `buckets`.`id` AS `bucket_id`, ";
		sql += "`buckets`.`bucket_name` ";
		sql += "FROM `buckets` ";
		sql += "INNER JOIN `buckets_droplets` ON (`buckets`.`id` = `buckets_droplets`.`bucket_id`) ";
		sql += "WHERE `buckets_droplets`.`id` IN (:dropIds)";
		sql += "AND `buckets`.`bucket_publish` = 1 ";
		sql += "UNION ALL ";
		sql += "SELECT `buckets_droplets`.`id` AS `id`, `buckets`.`id` AS `bucket_id`, ";
		sql += "`buckets`.`bucket_name` ";
		sql += "FROM `buckets` ";
		sql += "INNER JOIN `buckets_droplets` ON (`buckets`.`id` = `buckets_droplets`.`bucket_id`) ";
		sql += "LEFT JOIN `accounts` ON (`buckets`.`account_id` = `accounts`.`id` AND `buckets`.`account_id` = :accountId) ";
		sql += "LEFT JOIN `bucket_collaborators` ON (`bucket_collaborators`.`bucket_id` = `buckets`.`id` AND `bucket_collaborators`.`account_id` = :accountId)";
		sql += "WHERE `buckets_droplets`.`id` IN (:dropIds) ";
		sql += "AND `buckets`.`bucket_publish` = 0 ";
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("dropIds", dropsIndex.keySet());
		params.addValue("accountId", queryingAccount.getId());
		
		List<Map<String, Object>> results = this.jdbcTemplate.queryForList(sql, params);
	
		// Group the buckets by bucket id
		Map<Long, Bucket> buckets = new HashMap<Long, Bucket>();
		for (Map<String, Object> row: results) {
			
			Long dropId = ((BigInteger)row.get("id")).longValue();
			Drop drop = drops.get(dropsIndex.get(dropId));
			if (drop.getBuckets() == null) {
				drop.setBuckets(new ArrayList<Bucket>());
			}

			Long bucketId = ((BigInteger) row.get("bucket_id")).longValue();
			Bucket bucket = buckets.get(bucketId);
			if (bucket == null) {
				bucket = new Bucket();
				bucket.setId(bucketId);
				bucket.setName((String)row.get("bucket_name"));
				
				buckets.put(bucketId, bucket);
			}
			
			// Add bucket to the list of buckets
			if (!drop.getBuckets().contains(bucket)) {
				drop.getBuckets().add(bucket);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ushahidi.swiftriver.core.api.dao.DropDao#findCommentById(java.lang.Long)
	 */
	public DropComment findCommentById(Long commentId) {
		return this.em.find(DropComment.class, commentId);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ushahidi.swiftriver.core.api.dao.DropDao#deleteComment(com.ushahidi.swiftriver.core.model.DropComment)
	 */
	public void deleteComment(DropComment dropComment) {
		this.em.remove(dropComment);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ushahidi.swiftriver.core.api.dao.DropDao#addComment(com.ushahidi.swiftriver.core.model.Drop, com.ushahidi.swiftriver.core.model.Account, java.lang.String)
	 */
	public DropComment addComment(Drop drop, Account account, String commentText) {
		DropComment dropComment = new DropComment();
		
		dropComment.setDrop(drop);
		dropComment.setAccount(account);
		dropComment.setDeleted(false);
		dropComment.setCommentText(commentText);
		dropComment.setDateAdded(new Date());

		this.em.persist(dropComment);

		return dropComment;
	}

}
