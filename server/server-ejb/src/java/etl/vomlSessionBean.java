/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package etl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.NoResultException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.util.Date;
import java.util.UUID;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import org.networkforgood.xml.namespaces.voml.*;
import persistence.*;

/**
 *
 * @author dave
 */
@Stateless
public class vomlSessionBean implements vomlSessionLocal {
    @PersistenceContext
    private EntityManager em;

    public void loadVoml() {
        try {
            VolunteerOpportunities vo = new VolunteerOpportunities();
            JAXBContext jc = JAXBContext.newInstance( VolunteerOpportunities.class.getPackage().getName() );
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            vo = (VolunteerOpportunities) unmarshaller.unmarshal(new File("/Users/dave/Documents/iVolunteer/code/ivolunteer/test_data/handsonnetwork_restricted_mucked.xml"));
            List<VolunteerOpportunity> opps = vo.getVolunteerOpportunity();

            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Query organizationQuery = em.createNamedQuery("Organization.findByName");
            Query eventQuery = em.createNamedQuery("Event.findByTitle");
            Query locationQuery = em.createNamedQuery("Location.findByStreetZip");
            Query timestampQuery = em.createNamedQuery("Timestamp.findByTimestamp");

            for ( VolunteerOpportunity opp : opps )
            {
                System.out.println(opp.getTitle());

                //SponsoringOrganization sponsor = opp.getSponsoringOrganizations().getSponsoringOrganization().iterator().next();
                List<SponsoringOrganization> sponsors = opp.getSponsoringOrganizations().getSponsoringOrganization();
              
                HashSet<Organization> orgs = new HashSet<Organization>();
                for ( SponsoringOrganization sponsor : sponsors ) {
                    
                    Organization org;
                    boolean newOrg = false;
                    try {
                        organizationQuery.setParameter("name", sponsor.getName());
                        org = (Organization) organizationQuery.getSingleResult();
                    } catch (NoResultException nr) {
                        newOrg = true;
                        org = new Organization();
                        org.setId(UUID.randomUUID().toString());
                        org.setName(sponsor.getName());
                        em.persist(org);
                    }

                    String sponsorAddress = sponsor.getAddress1() + " " + sponsor.getAddress2();
                    persistence.Location loc;
                    boolean newLoc = false;
                    try {
                        locationQuery.setParameter("street", sponsorAddress);
                        locationQuery.setParameter("zip", sponsor.getZipOrPostalCode());
                        loc = (persistence.Location) locationQuery.getSingleResult();
                    } catch (NoResultException nr) {
                        newLoc = true;
                        loc = new persistence.Location();
                        loc.setId(UUID.randomUUID().toString());
                        loc.setStreet(sponsorAddress);
                        loc.setCity(sponsor.getCity());
                        loc.setState(sponsor.getStateOrProvince());
                        loc.setZip(sponsor.getZipOrPostalCode());
                        em.persist(loc);
                    }

                    if ( ! org.getLocationCollection().contains(loc) ) {
                        org.getLocationCollection().add(loc);
                    }

                    org.setDescription(sponsor.getDescription());
                    org.setEmail(sponsor.getEmail());
                    org.setUrl(sponsor.getURL());

                    String sponsorPhone = sponsor.getPhone();
                    if (sponsor.getExtension() != null) {
                        sponsorPhone = sponsorPhone + " ext " + sponsor.getExtension();
                    }

                    org.setPhone(sponsorPhone);

                    em.merge(org);

                    orgs.add(org);
                }

                Event ev = null;
                eventQuery.setParameter("title", opp.getTitle());
                List<Event> events = eventQuery.getResultList();
                for (Event event : events) {
                    if ( event.getOrganizationCollection().containsAll(orgs) ) {
                        ev = event;
                        break;
                    }
                }

                if ( ev == null ) {
                    ev = new Event();
                    ev.setId(UUID.randomUUID().toString());
                    ev.setTitle(opp.getTitle());
                    ev.setOrganizationCollection(orgs);
                    em.persist(ev);
                }
                else
                {
                    orgs.addAll(ev.getOrganizationCollection());
                    ev.setOrganizationCollection(orgs);
                }
                
                ev.setDescription(opp.getDescription());

                persistence.Location loc;
                org.networkforgood.xml.namespaces.voml.Location oppLoc = opp.getLocations().getLocation();
                String oppAddress = oppLoc.getAddress1() + " " + oppLoc.getAddress2();
                boolean newLoc = false;
                try {
                    locationQuery.setParameter("street", oppAddress);
                    locationQuery.setParameter("zip", oppLoc.getZipOrPostalCode());
                    loc = (persistence.Location) locationQuery.getSingleResult();
                } catch (NoResultException nr) {
                    newLoc = true;
                    loc = new persistence.Location();
                    loc.setId(UUID.randomUUID().toString());
                    loc.setStreet(oppAddress);
                    loc.setCity(oppLoc.getCity());
                    loc.setState(oppLoc.getStateOrProvince());
                    loc.setZip(oppLoc.getZipOrPostalCode());
                    em.persist(loc);
                }

                ev.getLocationCollection().add(loc);

                List<OpportunityDate> oppDates = opp.getOpportunityDates().getOpportunityDate();

                for (OpportunityDate oppDate : oppDates) {

                    try {
                        Date startDate = dateFormatter.parse(oppDate.getStartDate() + " " + oppDate.getStartTime());
                        Timestamp ts;
                        try {
                            timestampQuery.setParameter("timestamp", startDate);
                            ts = (Timestamp) timestampQuery.getSingleResult();
                        } catch (NoResultException nr) {
                            ts = new Timestamp();
                            ts.setId(UUID.randomUUID().toString());
                            ts.setTimestamp(startDate);
                            em.persist(ts);
                        }

                        ev.getTimestampCollection().add(ts);
                        
                        if ( oppDate.getDuration() != null ) {
                            String durUnits = oppDate.getDuration().getDurationUnit();


                        }
                        else
                        {
                            Date endDate = dateFormatter.parse(oppDate.getEndDate() + " " + oppDate.getEndTime());
                            long dur = (endDate.getTime() - startDate.getTime())/1000;
                            ev.setDuration((short) dur);
                        }
                    }
                    catch (ParseException pe) {
                        System.out.println(pe.toString());
                    }
                }
                em.merge(ev);
                em.flush();

            }
        }
        catch( UnmarshalException ue ) {
            System.out.println( "Caught UnmarshalException" );
            System.out.println(ue.toString());
        }
        catch( JAXBException je ) {
            je.printStackTrace();
        }        

    }

    public void persist(Object object) {
        em.persist(object);
    }

    
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method" or "Web Service > Add Operation")

    
 
}
