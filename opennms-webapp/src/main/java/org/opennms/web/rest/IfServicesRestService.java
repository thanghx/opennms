/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2012 The OpenNMS Group, Inc.
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

package org.opennms.web.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.opennms.core.criteria.Alias.JoinType;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.Order;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.dao.MonitoredServiceDao;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsMonitoredServiceDetail;
import org.opennms.netmgt.model.OnmsMonitoredServiceDetailList;
import org.opennms.netmgt.model.OnmsMonitoredServiceList;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.model.events.EventProxy;
import org.opennms.netmgt.model.events.EventProxyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

/**
 * Managing Monitored Services (control the polling state of monitored services).
 * 
 * Examples:
 *
 * curl -u admin:admin "http://localhost:8980/opennms/rest/ifservices?node.label=onms-prd-01"
 * curl -u admin:admin "http://localhost:8980/opennms/rest/ifservices?ipInterface.ipAddress=192.168.32.140"
 * curl -u admin:admin "http://localhost:8980/opennms/rest/ifservices?category.name=Production"
 * 
 * curl -X PUT "status=F" -u admin:admin "http://localhost:8980/opennms/rest/ifservices?node.label=onms-prd-01"
 * curl -X PUT "status=A" -u admin:admin "http://localhost:8980/opennms/rest/ifservices?ipInterface.ipAddress=192.168.32.140"
 * curl -X PUT "status=F" -u admin:admin "http://localhost:8980/opennms/rest/ifservices?category.name=Production"
 * curl -X PUT "status=F&services=ICMP,HTTP" -u admin:admin "http://localhost:8980/opennms/rest/ifservices?category.name=Production"
 * 
 * Possible values for status:
 * A (Managed)
 * F (Forced Unmanaged)
 * R (Rescan to Resume, for compatibility purposes)
 * S (Rescan to Suspend, for compatibility purposes)
 * 
 * The optional parameter services is designed to specify the list of affected services as CSV.
 *
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@Component
@PerRequest
@Scope("prototype")
@Path("ifservices")
@Transactional
public class IfServicesRestService extends OnmsRestService {

    @Autowired
    private MonitoredServiceDao m_serviceDao;

    @Autowired
    private EventProxy m_eventProxy;

    @Context 
    private UriInfo m_uriInfo;

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public OnmsMonitoredServiceDetailList getServices() {
        readLock();
        try {
            final Criteria c = getCriteria(m_uriInfo.getQueryParameters());
            final OnmsMonitoredServiceDetailList servicesList = new OnmsMonitoredServiceDetailList();
            final List<OnmsMonitoredService> services = m_serviceDao.findMatching(c);
            for (OnmsMonitoredService svc : services) {
                servicesList.add(new OnmsMonitoredServiceDetail(svc));
            }
            c.setLimit(null);
            c.setOffset(null);
            c.setOrders(new ArrayList<Order>());
            servicesList.setTotalCount(m_serviceDao.countMatching(c));
            return servicesList;
        } finally {
            readUnlock();
        }
    }

    @PUT
    public Response updateServices(MultivaluedMapImpl params) {
        readLock();
        try {
            final String status = params.getFirst("status");
            if (status == null || !status.matches("(A|R|S|F)")) {
                throw getException(Status.BAD_REQUEST, "updateServices: parameter status must be specified. Possible values: A (Managed), F (Forced Unmanaged), R (Rescan to Resume), S (Rescan to Suspend)");
            }
            final String services_csv = params.getFirst("services");
            final List<String> serviceList = new ArrayList<String>();
            if (services_csv != null) {
                for (String s : services_csv.split(",")) {
                    serviceList.add(s);
                }
            }
            final Criteria c = getCriteria(m_uriInfo.getQueryParameters());
            c.setLimit(null);
            c.setOffset(null);
            final OnmsMonitoredServiceList services = new OnmsMonitoredServiceList(m_serviceDao.findMatching(c));
            for (OnmsMonitoredService svc : services) {
                boolean proceed = false;
                if (serviceList.isEmpty()) {
                    proceed = true;
                } else {
                    if (serviceList.contains(svc.getServiceName())) {
                        proceed = true;
                    }
                }
                if (proceed) {
                    final String currentStatus = svc.getStatus();
                    svc.setStatus(status);
                    m_serviceDao.update(svc);
                    if ("S".equals(status) || (currentStatus.equals("A") && status.equals("F"))) {
                        log().debug("updateServices: suspending polling for service " + svc.getServiceName() + " on node with IP " + svc.getIpAddress().getHostAddress());
                        sendEvent(EventConstants.SUSPEND_POLLING_SERVICE_EVENT_UEI, svc);
                    }
                    if ("R".equals(status) || (currentStatus.equals("F") && status.equals("A"))) {
                        log().debug("updateServices: resumg polling for service " + svc.getServiceName() + " on node with IP " + svc.getIpAddress().getHostAddress());
                        sendEvent(EventConstants.RESUME_POLLING_SERVICE_EVENT_UEI, svc);
                    }
                }
            }
        } finally {
            readUnlock();
        }
        return Response.seeOther(getRedirectUri(m_uriInfo)).build();
    }

    private Criteria getCriteria(MultivaluedMap<String, String> params) {
        final CriteriaBuilder builder = new CriteriaBuilder(OnmsMonitoredService.class);
        builder.alias("ipInterface.snmpInterface", "snmpInterface", JoinType.LEFT_JOIN);
        builder.alias("ipInterface", "ipInterface", JoinType.LEFT_JOIN);
        builder.alias("ipInterface.node", "node", JoinType.LEFT_JOIN);
        builder.alias("ipInterface.node.categories", "category", JoinType.LEFT_JOIN);
        builder.alias("serviceType", "serviceType", JoinType.LEFT_JOIN);
        applyQueryFilters(params, builder);

        final Criteria criteria = builder.toCriteria();
        return criteria;
    }

    private void sendEvent(String eventUEI, OnmsMonitoredService dbObj) {
        final EventBuilder bldr = new EventBuilder(eventUEI, getClass().getName());
        bldr.setNodeid(dbObj.getNodeId());
        bldr.setInterface(dbObj.getIpAddress());
        bldr.setService(dbObj.getServiceName());
        try {
            m_eventProxy.send(bldr.getEvent());
        } catch (EventProxyException ex) {
            throw getException(Status.BAD_REQUEST, ex.getMessage());
        }
    }

}