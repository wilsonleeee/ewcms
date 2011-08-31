/**
 * Copyright (c)2010-2011 Enterprise Website Content Management System(EWCMS), All rights reserved.
 * EWCMS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.ewcms.com
 */

package com.ewcms.content.document.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.ewcms.common.dao.JpaDAO;
import com.ewcms.content.document.model.ReviewProcess;

/**
 * 审核流程DAO
 * 
 * @author wu_zhijun
 *
 */
@Repository
public class ReviewProcessDAO extends JpaDAO<Long, ReviewProcess> {
	
	@SuppressWarnings("unchecked")
	public Long findReviewProcessCountByChannel(Integer channelId){
		String hql = "Select Count(p.id) From ReviewProcess As p Where p.channelId=?";
		List<Long> list = this.getJpaTemplate().find(hql, channelId);
		if (list.isEmpty()) return 0L;
		return list.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public List<ReviewProcess> findReviewProcessByChannel(Integer channelId){
		String hql = "From ReviewProcess As p Where p.channelId=?";
		List<ReviewProcess> list = this.getJpaTemplate().find(hql, channelId);
		if (list.isEmpty()) return null;
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public ReviewProcess findReviewProcessByIdAndChannel(Long id, Integer channelId){
		String hql = "From ReviewProcess As p Where p.id=? And p.channelId=?";
		List<ReviewProcess> list = this.getJpaTemplate().find(hql, id, channelId);
		if (list.isEmpty()) return null;
		return list.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public ReviewProcess findLastReviewProcessByChannel(Integer channelId){
		String hql = "From ReviewProcess As p Where p.channelId=? And p.nextProcess Is Null";
		List<ReviewProcess> list = this.getJpaTemplate().find(hql, channelId);
		if (list.isEmpty()) return null;
		return list.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public ReviewProcess findFirstReviewProcessByChannel(Integer channelId){
		String hql = "From ReviewProcess As p Where p.channelId=? And p.prevProcess Is Null";
		List<ReviewProcess> list = this.getJpaTemplate().find(hql, channelId);
		if (list.isEmpty()) return null;
		return list.get(0);
	}
}