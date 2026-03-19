package de.bericht.service;

import java.util.HashMap;
import java.util.Map;

import de.bericht.util.BerichtHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Pfad für die API
@Path("/api/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusService {

	@Inject
	private BerichtHelper berichtHelper; // Deine Business-Logik Bean

	@GET
	@Path("{ergebnisLink}")
	public Response getStatus(@PathParam("ergebnisLink") String ergebnisLink, String vereinnr) {
		Map<String, Boolean> status = new HashMap<>();
		status.put("bericht", berichtHelper.hasBericht(vereinnr, ergebnisLink));
		status.put("bild", berichtHelper.hasBild(vereinnr, ergebnisLink));
		status.put("freigabe", berichtHelper.hasFreigabe(vereinnr, ergebnisLink));
		status.put("homepage", berichtHelper.hasHomepage(vereinnr, ergebnisLink));
		status.put("blaettle", berichtHelper.hasBlaettle(vereinnr, ergebnisLink));

		return Response.ok(status).build();
	}
}
