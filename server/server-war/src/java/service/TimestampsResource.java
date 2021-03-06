/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package service;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import javax.persistence.EntityManager;
import persistence.Event;
import converter.TimestampsConverter;
import converter.TimestampConverter;
import converter.TimestampListConverter;
import persistence.Timestamp;
import session.TimestampFacadeLocal;

/**
 *
 * @author dave
 */

@Path("/timestamps/")
public class TimestampsResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
  
    /** Creates a new instance of TimestampsResource */
    public TimestampsResource() {
    }

    /**
     * Get method for retrieving a collection of Timestamp instance in XML format.
     *
     * @return an instance of TimestampsConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public TimestampsConverter get(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel, @QueryParam("query")
    @DefaultValue("SELECT e FROM Timestamp e")
    String query) {
        try {
            return new TimestampsConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), expandLevel);
        } finally {
        }
    }

    /**
     * Post method for creating an instance of Timestamp using XML as the input format.
     *
     * @param data an TimestampConverter entity that is deserialized from an XML stream
     * @return an instance of TimestampConverter
     */
    @POST
    @Consumes({"application/xml", "application/json"})
    public Response post(TimestampConverter data) {
        try {
            Timestamp entity = data.getEntity();
            createEntity(entity);
            return Response.created(uriInfo.getAbsolutePath().resolve(entity.getId() + "/")).build();
        } finally {
        }
    }

    /**
     * Returns a dynamic instance of TimestampResource used for entity navigation.
     *
     * @return an instance of TimestampResource
     */
    @Path("{id}/")
    public service.TimestampResource getTimestampResource(@PathParam("id")
    String id) {
        TimestampResource resource = resourceContext.getResource(TimestampResource.class);
        resource.setId(id);
        return resource;
    }

    /**
     * Returns all the entities associated with this resource.
     *
     * @return a collection of Timestamp instances
     */
    protected Collection<Timestamp> getEntities(int start, int max, String query) {
        return lookupTimestampFacade().findAll(start, max);
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Timestamp entity) {
        lookupTimestampFacade().create(entity);
    }

    @Path("list/")
    @GET
    @Produces({"application/json"})
    public TimestampListConverter list(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("query")
    @DefaultValue("SELECT e FROM Event e")
    String query) {
        try {
            return new TimestampListConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), uriInfo.getBaseUri());
        } finally {

        }
    }

    private TimestampFacadeLocal lookupTimestampFacade() {
        try {
            javax.naming.Context c = new InitialContext();
            return (TimestampFacadeLocal) c.lookup("java:comp/env/TimestampFacade");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }
}
