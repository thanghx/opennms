/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.dao.hibernate;

import java.util.Date;
import java.util.List;

import org.opennms.netmgt.dao.StpNodeDao;
import org.opennms.netmgt.model.OnmsArpInterface.StatusType;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsStpNode;

public class StpNodeDaoHibernate extends AbstractDaoHibernate<OnmsStpNode, Integer>  implements StpNodeDao {
    
    public StpNodeDaoHibernate() {
        super(OnmsStpNode.class);
    }

	@Override
	public void markDeletedIfNodeDeleted() {
        final String jql = "from OnmsStpNode left join node where node.type = :nodeType";
        List<OnmsStpNode> stpNodeList = getJpaTemplate().getEntityManager()
                .createQuery(jql)
                .setParameter("nodeType", "D")
                .getResultList();

        // TODO MVR JPA verify with Simon
//		final OnmsCriteria criteria = new OnmsCriteria(OnmsStpNode.class);
//        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
//        criteria.add(Restrictions.eq("node.type", "D"));
        
        for (final OnmsStpNode stpNode : stpNodeList) {
        	stpNode.setStatus(StatusType.DELETED);
        	saveOrUpdate(stpNode);
        }
	}

    @Override
    public void deactivateForNodeIdIfOlderThan(final int nodeId, final Date scanTime) {
        final String jql = "from OnmsStpNode left join node where node.id = :nodeId and lastPollTime < : scanTime and status = :status";
        List<OnmsStpNode> stpNodeList = getJpaTemplate().getEntityManager()
                .createQuery(jql)
                .setParameter("nodeId", nodeId)
                .setParameter("lastPollTime", scanTime)
                .setParameter("status", StatusType.ACTIVE)
                .getResultList();


        // TODO MVR JPA verify with Simon
//        final OnmsCriteria criteria = new OnmsCriteria(OnmsStpNode.class);
//        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
//        criteria.add(Restrictions.eq("node.id", nodeid));
//        criteria.add(Restrictions.lt("lastPollTime", scanTime));
//        criteria.add(Restrictions.eq("status", StatusType.ACTIVE));
        
        for (final OnmsStpNode item : stpNodeList) {
            item.setStatus(StatusType.INACTIVE);
            saveOrUpdate(item);
        }
    }

    @Override
    public void deleteForNodeIdIfOlderThan(final int nodeId, final Date scanTime) {
        final String jql = "from OnmsStpNode left join node where node.id = :nodeId and lastPollTime < : scanTime and status <> :status";
        List<OnmsStpNode> stpNodeList = getJpaTemplate().getEntityManager()
                .createQuery(jql)
                .setParameter("nodeId", nodeId)
                .setParameter("lastPollTime", scanTime)
                .setParameter("status", StatusType.ACTIVE)
                .getResultList();

        // TODO MVR JPA verify with Simon
//        final OnmsCriteria criteria = new OnmsCriteria(OnmsStpNode.class);
//        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
//        criteria.add(Restrictions.eq("node.id", nodeid));
//        criteria.add(Restrictions.lt("lastPollTime", scanTime));
//        criteria.add(Restrictions.not(Restrictions.eq("status", StatusType.ACTIVE)));
        
        for (final OnmsStpNode item : stpNodeList) {
            delete(item);
        }
    }


    @Override
    public void setStatusForNode(final Integer nodeId, final StatusType action) {
        final String jql = "fron OnmsStpNode left join node where node.id = :nodeId";
        List<OnmsStpNode> stpNodes = getJpaTemplate().getEntityManager()
                .createQuery(jql)
                .setParameter("nodeId", nodeId)
                .getResultList();

        // TODO MVR JPA verify with Simon
//        final OnmsCriteria criteria = new OnmsCriteria(OnmsStpNode.class);
//        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
//        criteria.add(Restrictions.eq("node.id", nodeid));
        
        for (final OnmsStpNode item : stpNodes) {
            item.setStatus(action);
            saveOrUpdate(item);
        }
    }

    @Override
    public OnmsStpNode findByNodeAndVlan(final Integer nodeId, final Integer baseVlan) {
        final String jql = "from OnmsStpNode left join node where node.id = :nodeId and baseVlan = :baseVlan";
        return (OnmsStpNode) getJpaTemplate().getEntityManager()
                .createQuery(jql)
                .setParameter("nodeId", nodeId)
                .setParameter("baseVlan", baseVlan)
                .setMaxResults(1)
                .getSingleResult();

        // TODO MVR JPA verify with Simon
//        final OnmsCriteria criteria = new OnmsCriteria(OnmsStpNode.class);
//        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
//        criteria.add(Restrictions.eq("node.id", nodeId));
//        criteria.add(Restrictions.eq("baseVlan", baseVlan));
//
//        final List<OnmsStpNode> stpNodes = findMatching(criteria);
//        if (stpNodes != null && stpNodes.size() > 0) {
//            return stpNodes.get(0);
//        }
//        return null;
    }
}
