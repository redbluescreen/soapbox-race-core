/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.*;
import com.soapboxrace.core.jpa.EventEntity;
import com.soapboxrace.core.jpa.EventMode;
import com.soapboxrace.core.jpa.EventSessionEntity;
import com.soapboxrace.jaxb.http.*;
import com.soapboxrace.jaxb.util.MarshalXML;
import com.soapboxrace.jaxb.util.UnmarshalXML;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/event")
public class Event {

    @EJB
    private TokenSessionBO tokenBO;

    @EJB
    private EventBO eventBO;

    @EJB
    private EventResultDragBO eventResultDragBO;

    @EJB
    private EventResultPursuitBO eventResultPursuitBO;

    @EJB
    private EventResultRouteBO eventResultRouteBO;

    @EJB
    private EventResultTeamEscapeBO eventResultTeamEscapeBO;

    @EJB
    private MatchmakingBO matchmakingBO;

    @POST
    @Secured
    @Path("/abort")
    @Produces(MediaType.APPLICATION_XML)
    public String abort(@QueryParam("eventSessionId") Long eventSessionId) {
        return "";
    }

    @PUT
    @Secured
    @Path("/launched")
    @Produces(MediaType.APPLICATION_XML)
    public String launched(@HeaderParam("securityToken") String securityToken,
                           @QueryParam("eventSessionId") Long eventSessionId) {
        Long activePersonaId = tokenBO.getActivePersonaId(securityToken);
        matchmakingBO.removePlayerFromQueue(activePersonaId);
        eventBO.createEventDataSession(activePersonaId, eventSessionId);
        return "";
    }

    @POST
    @Secured
    @Path("/arbitration")
    @Produces(MediaType.APPLICATION_XML)
    public String arbitration(InputStream arbitrationXml, @HeaderParam("securityToken") String securityToken,
                              @QueryParam("eventSessionId") Long eventSessionId) {
        EventSessionEntity eventSessionEntity = eventBO.findEventSessionById(eventSessionId);
        EventEntity event = eventSessionEntity.getEvent();
        EventMode eventMode = EventMode.fromId(event.getEventModeId());
        Long activePersonaId = tokenBO.getActivePersonaId(securityToken);
        EventResult eventResult = null;

        switch (eventMode) {
            case CIRCUIT:
            case SPRINT:
                RouteArbitrationPacket routeArbitrationPacket = UnmarshalXML.unMarshal(arbitrationXml,
                        RouteArbitrationPacket.class);
                eventResult = eventResultRouteBO.handle(eventSessionEntity, activePersonaId,
                        routeArbitrationPacket);
                break;
            case DRAG:
                DragArbitrationPacket dragArbitrationPacket = UnmarshalXML.unMarshal(arbitrationXml,
                        DragArbitrationPacket.class);
                eventResult = eventResultDragBO.handle(eventSessionEntity, activePersonaId, dragArbitrationPacket);
                break;
            case PURSUIT_MP:
                TeamEscapeArbitrationPacket teamEscapeArbitrationPacket = UnmarshalXML.unMarshal(arbitrationXml,
                        TeamEscapeArbitrationPacket.class);
                eventResult = eventResultTeamEscapeBO.handle(eventSessionEntity, activePersonaId,
                        teamEscapeArbitrationPacket);
                break;
            case PURSUIT_SP:
                PursuitArbitrationPacket pursuitArbitrationPacket = UnmarshalXML.unMarshal(arbitrationXml,
                        PursuitArbitrationPacket.class);
                eventResult = eventResultPursuitBO.handle(eventSessionEntity, activePersonaId,
                        pursuitArbitrationPacket);
                break;
            case MEETINGPLACE:
            default:
                break;
        }

        if (eventResult == null) {
            return "";
        }

        return MarshalXML.marshal(eventResult);
    }

    @POST
    @Secured
    @Path("/bust")
    @Produces(MediaType.APPLICATION_XML)
    public String bust(InputStream bustXml, @HeaderParam("securityToken") String securityToken, @QueryParam(
            "eventSessionId") Long eventSessionId) {
        EventSessionEntity eventSessionEntity = eventBO.findEventSessionById(eventSessionId);
        PursuitArbitrationPacket pursuitArbitrationPacket = UnmarshalXML.unMarshal(bustXml,
                PursuitArbitrationPacket.class);
        Long activePersonaId = tokenBO.getActivePersonaId(securityToken);
        return MarshalXML.marshal(eventResultPursuitBO.handle(eventSessionEntity, activePersonaId, pursuitArbitrationPacket));
    }
}
