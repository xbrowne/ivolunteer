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
import persistence.Organization;
import persistence.Source;
import persistence.InterestArea;
import persistence.Event;
import persistence.OrganizationType;
import persistence.Location;
import converter.OrganizationsConverter;
import converter.OrganizationConverter;
import converter.OrganizationListConverter;
import session.OrganizationFacadeLocal;

/**
 *
 * @author dave
 */

@Path("/organizations/")
public class OrganizationsResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
  
    /** Creates a new instance of OrganizationsResource */
    public OrganizationsResource() {
    }

    /**
     * Get method for retrieving a collection of Organization instance in XML format.
     *
     * @return an instance of OrganizationsConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public OrganizationsConverter get(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel, @QueryParam("query")
    @DefaultValue("SELECT e FROM Organization e")
    String query) {
        try {
            return new OrganizationsConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), expandLevel);
        } finally {
        }
    }

    /**
     * Post method for creating an instance of Organization using XML as the input format.
     *
     * @param data an OrganizationConverter entity that is deserialized from an XML stream
     * @return an instance of OrganizationConverter
     */
    @POST
    @Consumes({"application/xml", "application/json"})
    public Response post(OrganizationConverter data) {
        try {
            Organization entity = data.getEntity();
            createEntity(entity);
            return Response.created(uriInfo.getAbsolutePath().resolve(entity.getId() + "/")).build();
        } finally {
        }
    }

    /**
     * Returns a dynamic instance of OrganizationResource used for entity navigation.
     *
     * @return an instance of OrganizationResource
     */
    @Path("{id}/")
    public service.OrganizationResource getOrganizationResource(@PathParam("id")
    String id) {
        OrganizationResource resource = resourceContext.getResource(OrganizationResource.class);
        resource.setId(id);
        return resource;
    }

    /**
     * Returns all the entities associated with this resource.
     *
     * @return a collection of Organization instances
     */
    protected Collection<Organization> getEntities(int start, int max, String query) {
        return lookupOrganizationFacade().findAll(start,max);
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Organization entity) {
        lookupOrganizationFacade().create(entity);
    }

    @Path("list/")
    @GET
    @Produces({"application/json"})
    public OrganizationListConverter list(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("query")
    @DefaultValue("SELECT e FROM Organization e")
    String query) {
        try {
            return new OrganizationListConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), uriInfo.getBaseUri());
        } finally {

        }
    }

    private OrganizationFacadeLocal lookupOrganizationFacade() {
        try {
            javax.naming.Context c = new InitialContext();
            return (OrganizationFacadeLocal) c.lookup("java:comp/env/OrganizationFacade");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }
}
